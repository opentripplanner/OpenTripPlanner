package org.opentripplanner.ext.carpooling.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.EndpointLabel;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingMethod;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps carpooling insertion candidates to OTP itineraries for API responses.
 * <p>
 * This mapper bridges between the carpooling domain model ({@link InsertionCandidate}) and
 * OTP's standard itinerary model ({@link Itinerary}). It extracts the passenger's journey
 * portion from the complete driver route and constructs an itinerary with timing, geometry,
 * and cost information.
 *
 * <h2>Itinerary Shape</h2>
 * <p>
 * Both direct-mode and access/egress itineraries have between one and three legs, emitted in
 * chronological order:
 * <ol>
 *   <li>An optional leading {@link StreetLeg} (WALK) from the passenger-side location (origin
 *       for direct/access, transit stop for egress) to the snapped carpool pickup vertex.</li>
 *   <li>A {@link CarpoolLeg} covering the shared ride between pickup and dropoff. The
 *       boarding dwell at the pickup is included in this leg's duration, not added before it.</li>
 *   <li>An optional trailing {@link StreetLeg} (WALK) from the snapped dropoff vertex to the
 *       dropoff-side location (destination for direct/egress, transit stop for access).</li>
 * </ol>
 *
 * <h2>Time Calculation</h2>
 * <p>
 * The carpool leg's start time is the moment the driver arrives at the pickup
 * (trip start + pickup travel). If a leading walk leg exists, its start is back-shifted by the
 * walk's duration; the trailing walk leg's end is forward-shifted similarly. The itinerary is
 * <em>not</em> shifted to match the passenger's requested departure time — the driver is on a
 * committed schedule and cannot wait. Whether the passenger should show up early or be matched
 * at all is a filtering concern upstream of this mapper.
 *
 * <h2>Place naming</h2>
 * <p>
 * The itinerary's outermost endpoints (first leg's {@code from}, last leg's {@code to}) are
 * resolved with this precedence:
 * <ol>
 *   <li>If the endpoint is a transit stop (access/egress), label with the stop's name via
 *       {@link Place#forStop(StopLocation)}.</li>
 *   <li>Else if the endpoint is the user's input location, label with the user-supplied
 *       label when present, falling back to the localized {@code "origin"} /
 *       {@code "destination"} names that the standard request flow produces.
 *   <li>Else (intermediate boundaries between walk and carpool legs) fall back to vertex-based
 *       naming via {@link #makePlace(Vertex)}: {@link StreetVertex#getIntersectionName()} for
 *       street intersections (composed from OSM street names; mode-agnostic), the vertex's
 *       pre-set name for {@link TemporaryStreetLocation}s.</li>
 * </ol>
 *
 * <h2>Package Location</h2>
 * <p>
 * This class is in the {@code internal} package because it's an implementation detail of
 * the carpooling service. API consumers interact with {@link Itinerary} objects, not this mapper.
 *
 * @see InsertionCandidate for the source data structure
 * @see CarpoolLeg for the carpool-specific leg type
 * @see Itinerary for the OTP itinerary model
 */
public class CarpoolItineraryMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolItineraryMapper.class);
  private static final I18NString ORIGIN_DEFAULT_NAME = new LocalizedString("origin");
  private static final I18NString DESTINATION_DEFAULT_NAME = new LocalizedString("destination");

  /**
   * Converts an insertion candidate into an OTP itinerary representing the passenger's journey.
   * The itinerary contains a {@link CarpoolLeg} for the shared ride, optionally preceded by a
   * WALK {@link StreetLeg} from the passenger's origin to the snapped pickup vertex and
   * optionally followed by a WALK leg from the snapped dropoff vertex to the destination.
   *
   * @param candidate the insertion candidate containing route segments, trip details, and
   *        optional walk paths around the carpool pickup/dropoff
   * @param carpoolReluctance multiplier applied to ride seconds when computing the carpool leg's
   *        {@code generalizedCost}.
   * @param fromLocation the request's {@code from} location (passenger origin), used to label
   *        the first leg's {@code from} place.
   * @param toLocation the request's {@code to} location (passenger destination), used to label
   *        the last leg's {@code to} place.
   * @return an itinerary for the passenger's journey, or {@code null} if the candidate has no
   *         shared segments (a safety check that should not trigger for valid candidates)
   */
  @Nullable
  public Itinerary toItinerary(
    InsertionCandidate candidate,
    double carpoolReluctance,
    GenericLocation fromLocation,
    GenericLocation toLocation
  ) {
    var sharedSegments = candidate.getSharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }
    var carpoolStart = candidate.trip().startTime().plus(candidate.getDurationUntilPickupArrival());
    var carpoolEnd = carpoolStart.plus(candidate.getPassengerRideDuration());
    return buildItinerary(
      sharedSegments,
      candidate.walkToPickup(),
      candidate.walkFromDropoff(),
      carpoolStart,
      carpoolEnd,
      candidate.getPassengerRideWeight(carpoolReluctance),
      EndpointLabel.forLocation(fromLocation),
      EndpointLabel.forLocation(toLocation),
      toBookingInfo(
        candidate.trip().publicContactInformation(),
        candidate.trip().startTime(),
        boardingCoordinate(sharedSegments),
        alightingCoordinate(sharedSegments)
      )
    );
  }

  private static CarpoolLeg buildCarpoolLeg(
    List<GraphPath<State, Edge, Vertex>> sharedSegments,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    double rideWeight,
    Place fromPlace,
    Place toPlace,
    @Nullable BookingInfo bookingInfo
  ) {
    var allEdges = sharedSegments
      .stream()
      .flatMap(seg -> seg.edges.stream())
      .toList();

    return CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(fromPlace)
      .withTo(toPlace)
      .withGeometry(GeometryUtils.concatenateLineStrings(allEdges, Edge::getGeometry))
      .withDistanceMeters(allEdges.stream().mapToDouble(Edge::getDistanceMeters).sum())
      .withGeneralizedCost((int) rideWeight)
      .withPickupBookingInfo(bookingInfo)
      .build();
  }

  private static StreetLeg buildWalkLeg(
    GraphPath<State, Edge, Vertex> walkPath,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    Place fromPlace,
    Place toPlace
  ) {
    LineString geometry = GeometryUtils.concatenateLineStrings(walkPath.edges, Edge::getGeometry);
    double distance = walkPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();

    return StreetLeg.of()
      .withMode(TraverseMode.WALK)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(fromPlace)
      .withTo(toPlace)
      .withGeometry(geometry)
      .withDistanceMeters(distance)
      .withGeneralizedCost((int) walkPath.getWeight())
      .build();
  }

  /**
   * Converts a Raptor carpool access/egress leg back into an OTP itinerary for the response. Same
   * shape as {@link #toItinerary(InsertionCandidate, double, GenericLocation, GenericLocation)} —
   * an optional WALK leg, the carpool leg, and an optional WALK leg — but driven from the
   * {@link CarpoolAccessEgress} accessors so the displayed times and ride cost agree with the
   * values Raptor used during the search.
   *
   * @return the itinerary, or {@code null} if the access/egress has no shared segments (a safety
   *         check that should not trigger for valid legs)
   */
  @Nullable
  public Itinerary toItinerary(CarpoolAccessEgress accessEgress) {
    var sharedSegments = accessEgress.sharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }
    return buildItinerary(
      sharedSegments,
      accessEgress.walkToPickup(),
      accessEgress.walkFromDropoff(),
      accessEgress.getCarpoolStart(),
      accessEgress.getCarpoolEnd(),
      accessEgress.getPassengerRideWeight(),
      accessEgress.startLabel(),
      accessEgress.endLabel(),
      toBookingInfo(
        accessEgress.trip().publicContactInformation(),
        accessEgress.trip().startTime(),
        boardingCoordinate(sharedSegments),
        alightingCoordinate(sharedSegments)
      )
    );
  }

  /**
   * Assembles a WALK + CARPOOL + WALK itinerary around the shared ride. Walk legs are omitted
   * when their corresponding path is {@code null}; the walk legs' outer times are derived from
   * {@code carpoolStart}/{@code carpoolEnd} plus the walk durations.
   * <p>
   * Boundary-place precedence at the chain's outermost endpoints follows the {@link EndpointLabel}
   * contract: a stop wins over a user location, which wins over the vertex-derived fallback name
   * via {@link #makePlace(Vertex)}.
   */
  private static Itinerary buildItinerary(
    List<GraphPath<State, Edge, Vertex>> sharedSegments,
    @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
    @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff,
    ZonedDateTime carpoolStart,
    ZonedDateTime carpoolEnd,
    double rideWeight,
    EndpointLabel startLabel,
    EndpointLabel endLabel,
    @Nullable BookingInfo bookingInfo
  ) {
    Vertex pickupVertex = sharedSegments.getFirst().states.getFirst().getVertex();
    Vertex dropoffVertex = sharedSegments.getLast().states.getLast().getVertex();
    Place pickupPlace = makePlace(pickupVertex);
    Place dropoffPlace = makePlace(dropoffVertex);

    Vertex startBoundaryVertex = walkToPickup != null
      ? walkToPickup.states.getFirst().getVertex()
      : pickupVertex;
    Vertex endBoundaryVertex = walkFromDropoff != null
      ? walkFromDropoff.states.getLast().getVertex()
      : dropoffVertex;

    Place itineraryStart = boundaryPlace(startLabel, ORIGIN_DEFAULT_NAME, startBoundaryVertex);
    Place itineraryEnd = boundaryPlace(endLabel, DESTINATION_DEFAULT_NAME, endBoundaryVertex);

    Place carpoolFromPlace = walkToPickup != null ? pickupPlace : itineraryStart;
    Place carpoolToPlace = walkFromDropoff != null ? dropoffPlace : itineraryEnd;

    List<Leg> legs = new ArrayList<>(3);
    if (walkToPickup != null) {
      var walkStart = carpoolStart.minusSeconds(walkToPickup.getDuration());
      legs.add(buildWalkLeg(walkToPickup, walkStart, carpoolStart, itineraryStart, pickupPlace));
    }
    legs.add(
      buildCarpoolLeg(
        sharedSegments,
        carpoolStart,
        carpoolEnd,
        rideWeight,
        carpoolFromPlace,
        carpoolToPlace,
        bookingInfo
      )
    );
    if (walkFromDropoff != null) {
      var walkEnd = carpoolEnd.plusSeconds(walkFromDropoff.getDuration());
      legs.add(buildWalkLeg(walkFromDropoff, carpoolEnd, walkEnd, dropoffPlace, itineraryEnd));
    }

    int totalCost = legs.stream().mapToInt(Leg::generalizedCost).sum();
    return Itinerary.ofDirect(legs).withGeneralizedCost(Cost.costOfSeconds(totalCost)).build();
  }

  /**
   * Builds the carpool leg's {@code pickupBookingInfo} from the trip's public-contact details.
   * <p>
   * The contact's {@code bookingUrl} (if present) is augmented with {@code from_coordinate} and
   * {@code to_coordinate} query parameters reflecting the passenger's carpool boarding and
   * alighting vertices — i.e. where the passenger gets in/out of the driver's car. These are
   * distinct from the passenger's walking endpoints (handled by the surrounding walk legs) and
   * from the driver's trip origin/destination.
   * <p>
   * If the contact's booking URL cannot be parsed as a valid {@link URI}, the URL is dropped from
   * the returned {@code BookingInfo} (a malformed URL is logged as a warning and treated as if
   * the trip published no booking URL at all) and {@link BookingMethod#ONLINE} is omitted from
   * the booking methods. This keeps the "non-null return ⇒ at least one usable booking method"
   * contract honest: a {@code BookingInfo} is never returned advertising {@code ONLINE} without
   * a URL the user can actually open.
   * <p>
   * {@code latestBookingTime} is a temporary placeholder: how a real booking deadline should be
   * sourced for carpool trips is yet to be decided, so it is approximated by the trip's
   * departure time-of-day at {@code daysPrior=0} — good enough until the source is settled. The
   * {@code daysPrior=0} value is also load-bearing for the Transmodel API:
   * {@code BookingArrangement.bookWhen} returns {@code "advanceAndDayOfTravel"} when
   * {@code latestBookingTime.daysPrior == 0}, but collapses to {@code "timeOfTravelOnly"} if
   * {@code latestBookingTime} is null — which would misrepresent the carpool product as not
   * bookable in advance. See
   * {@code org.opentripplanner.apis.transmodel.mapping.BookingInfoMapper#mapToBookWhen}.
   *
   * @param contact the trip's public-contact details, or {@code null} if the trip publishes none.
   * @param tripStartTime the driver's trip start time; only the time-of-day is used, as the
   *        placeholder source for {@code latestBookingTime}.
   * @param pickup the carpool boarding coordinate (where the passenger gets into the car), used to
   *        augment the booking URL with {@code from_coordinate}.
   * @param dropoff the carpool alighting coordinate (where the passenger gets out of the car),
   *        used to augment the booking URL with {@code to_coordinate}.
   * @return a booking info populated with the contact details and derived booking methods
   *         (CALL_OFFICE if phone is set, ONLINE if URL is set and parses), or {@code null} when
   *         no actionable booking method can be derived — i.e. {@code contact} is {@code null},
   *         the contact carries neither a phone number nor a booking URL, or the only booking
   *         channel was a malformed URL. Consumers may therefore treat a non-null return as
   *         "this leg has at least one booking method."
   */
  @Nullable
  static BookingInfo toBookingInfo(
    @Nullable ContactInfo contact,
    ZonedDateTime tripStartTime,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    if (contact == null || (contact.getPhoneNumber() == null && contact.getBookingUrl() == null)) {
      return null;
    }
    var bookingMethods = EnumSet.noneOf(BookingMethod.class);
    if (contact.getPhoneNumber() != null) {
      bookingMethods.add(BookingMethod.CALL_OFFICE);
    }
    String effectiveUrl = contact.getBookingUrl() == null
      ? null
      : appendPassengerCoordinates(contact.getBookingUrl(), pickup, dropoff);
    if (effectiveUrl != null) {
      bookingMethods.add(BookingMethod.ONLINE);
    }
    if (bookingMethods.isEmpty()) {
      return null;
    }
    ContactInfo effectiveContact = Objects.equals(effectiveUrl, contact.getBookingUrl())
      ? contact
      : contact.copy().withBookingUrl(effectiveUrl).build();

    return BookingInfo.of()
      .withContactInfo(effectiveContact)
      .withBookingMethods(bookingMethods)
      .withLatestBookingTime(new BookingTime(tripStartTime.toLocalTime(), 0))
      .build();
  }

  private static WgsCoordinate boardingCoordinate(
    List<GraphPath<State, Edge, Vertex>> sharedSegments
  ) {
    return sharedSegments.getFirst().states.getFirst().getVertex().toWgsCoordinate();
  }

  private static WgsCoordinate alightingCoordinate(
    List<GraphPath<State, Edge, Vertex>> sharedSegments
  ) {
    return sharedSegments.getLast().states.getLast().getVertex().toWgsCoordinate();
  }

  /**
   * Appends {@code from_coordinate} and {@code to_coordinate} query parameters to the booking URL
   * so the carpool provider's booking page can pre-fill the passenger's pickup and dropoff.
   * <p>
   * The URL is parsed via {@link URI} so that any existing query string is merged with {@code &}
   * (rather than a second {@code ?}) and any fragment ends up after the appended parameters
   * rather than swallowing them.
   *
   * @return the augmented URL, or {@code null} if the input is not a parseable URI — in which
   *         case a warning is logged and the caller should drop the URL (and the
   *         {@link BookingMethod#ONLINE} booking method along with it).
   */
  @Nullable
  private static String appendPassengerCoordinates(
    String url,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    String addedQuery = String.format(
      Locale.ROOT,
      "from_coordinate=%.6f,%.6f&to_coordinate=%.6f,%.6f",
      pickup.latitude(),
      pickup.longitude(),
      dropoff.latitude(),
      dropoff.longitude()
    );
    try {
      URI uri = new URI(url);
      String existingQuery = uri.getQuery();
      String mergedQuery = existingQuery == null ? addedQuery : existingQuery + "&" + addedQuery;
      return new URI(
        uri.getScheme(),
        uri.getAuthority(),
        uri.getPath(),
        mergedQuery,
        uri.getFragment()
      ).toString();
    } catch (URISyntaxException e) {
      LOG.warn("Failed to parse carpool booking URL '{}'; dropping URL from booking info", url, e);
      return null;
    }
  }

  /**
   * Resolves the outermost endpoint of an itinerary using the precedence transit stop &gt;
   * user input location &gt; vertex-derived name. The first two are needed because the
   * underlying State chain doesn't always carry a vertex with the right name — the transit-side
   * end of an access/egress walk is a regular street vertex, and when no walk is needed at all
   * the State chain skips the user's temporary origin/destination vertex and starts straight at
   * the snapped pickup/dropoff (which has only an OSM intersection name).
   */
  private static Place boundaryPlace(
    EndpointLabel label,
    I18NString locationDefaultName,
    Vertex fallbackVertex
  ) {
    if (label.stop() != null) {
      return Place.forStop(label.stop());
    }
    if (label.location() != null) {
      // Place.forGenericLocation only swaps in defaultName when label() is null, but
      // GenericLocationMapper coerces an absent GraphQL "name" to "" — so without StringUtils'
      // null/blank check the Place name would render as a blank string instead of the localized
      // "origin"/"destination" fallback.
      var loc = label.location();
      I18NString name = StringUtils.hasValue(loc.label())
        ? new NonLocalizedString(loc.label())
        : locationDefaultName;
      return loc.wgsCoordinate() != null
        ? Place.normal(loc.wgsCoordinate(), name)
        : Place.noCoords(name);
    }
    return makePlace(fallbackVertex);
  }

  /**
   * Builds a {@link Place} from a vertex using the same naming logic the core street-leg
   * mapper applies for any mode. {@link StreetVertex} intersections get the localized
   * "corner of X and Y" name composed from the outgoing OSM street names (mode-agnostic);
   * {@link TemporaryStreetLocation}s (passenger origin/destination) keep the name they were
   * created with; everything else falls back to {@link Vertex#getName()}.
   */
  private static Place makePlace(Vertex vertex) {
    if (vertex instanceof StreetVertex sv && !(vertex instanceof TemporaryStreetLocation)) {
      return Place.normal(vertex, sv.getIntersectionName());
    }
    return Place.normal(vertex, vertex.getName());
  }
}
