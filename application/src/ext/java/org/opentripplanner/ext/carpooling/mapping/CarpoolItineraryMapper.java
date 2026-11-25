package org.opentripplanner.ext.carpooling.mapping;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.ext.carpooling.model.BookingUrlTemplate;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingMethod;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;
import org.opentripplanner.utils.lang.StringUtils;

/**
 * Maps a passenger's carpool ride to an itinerary of one to three legs:
 * <ol>
 *   <li>An optional WALK {@link StreetLeg} to the pickup.</li>
 *   <li>A {@link CarpoolLeg} from the pickup to the dropoff. It starts when the driver arrives at
 *       the pickup, so the boarding dwell is part of the leg.</li>
 *   <li>An optional WALK {@link StreetLeg} from the dropoff.</li>
 * </ol>
 * The itinerary is not shifted to the passenger's requested time: the driver is on a committed
 * schedule and cannot wait.
 * <p>
 * The itinerary's first {@code from} and last {@code to} are named, in order of precedence:
 * <ol>
 *   <li>after the transit stop, at the transit end of an access or egress ride;</li>
 *   <li>with the label of the user's input location, or "origin" / "destination" if it has
 *       none;</li>
 *   <li>after the street vertex, as the places between a walk leg and the carpool leg are.</li>
 * </ol>
 */
public class CarpoolItineraryMapper {

  private static final I18NString ORIGIN_DEFAULT_NAME = new LocalizedString("origin");
  private static final I18NString DESTINATION_DEFAULT_NAME = new LocalizedString("destination");

  /**
   * The itinerary of a direct ride, from the request's {@code fromLocation} to its
   * {@code toLocation}. See {@link InsertionCandidate#getPassengerRideWeight} for the two costs.
   */
  public Itinerary toItinerary(
    InsertionCandidate candidate,
    double carpoolReluctance,
    int boardCost,
    GenericLocation fromLocation,
    GenericLocation toLocation
  ) {
    return buildItinerary(
      candidate,
      candidate.getPassengerRideWeight(carpoolReluctance, boardCost),
      EndpointLabel.forLocation(fromLocation),
      EndpointLabel.forLocation(toLocation)
    );
  }

  /** The itinerary of an access or egress ride, with the ride cost Raptor searched with. */
  public Itinerary toItinerary(CarpoolAccessEgress accessEgress) {
    return buildItinerary(
      accessEgress.insertionCandidate(),
      accessEgress.getPassengerRideWeight(),
      accessEgress.startLabel(),
      accessEgress.endLabel()
    );
  }

  private static Itinerary buildItinerary(
    InsertionCandidate candidate,
    double rideWeight,
    EndpointLabel startLabel,
    EndpointLabel endLabel
  ) {
    var pathsWithNewPassenger = candidate.getPathsWithNewPassenger();
    var walkToPickup = candidate.walkToPickup();
    var walkFromDropoff = candidate.walkFromDropoff();
    var carpoolStart = candidate.pickupTime();
    var carpoolEnd = candidate.dropoffTime();

    Vertex pickupVertex = pathsWithNewPassenger.getFirst().states().getFirst().getVertex();
    Vertex dropoffVertex = pathsWithNewPassenger.getLast().lastState().getVertex();
    Place pickupPlace = makePlace(pickupVertex);
    Place dropoffPlace = makePlace(dropoffVertex);

    Vertex startBoundaryVertex =
      walkToPickup != null ? walkToPickup.states().getFirst().getVertex() : pickupVertex;
    Vertex endBoundaryVertex =
      walkFromDropoff != null ? walkFromDropoff.lastState().getVertex() : dropoffVertex;

    Place itineraryStart = boundaryPlace(startLabel, ORIGIN_DEFAULT_NAME, startBoundaryVertex);
    Place itineraryEnd = boundaryPlace(endLabel, DESTINATION_DEFAULT_NAME, endBoundaryVertex);

    Place carpoolFromPlace = walkToPickup != null ? pickupPlace : itineraryStart;
    Place carpoolToPlace = walkFromDropoff != null ? dropoffPlace : itineraryEnd;

    var bookingInfo = toBookingInfo(
      candidate.trip().publicContactInformation(),
      candidate.trip().startTime(),
      pickupVertex.toWgsCoordinate(),
      dropoffVertex.toWgsCoordinate()
    );

    List<Leg> legs = new ArrayList<>(3);
    if (walkToPickup != null) {
      var walkStart = carpoolStart.minus(walkToPickup.duration());
      legs.add(buildWalkLeg(walkToPickup, walkStart, carpoolStart, itineraryStart, pickupPlace));
    }
    legs.add(
      buildCarpoolLeg(
        pathsWithNewPassenger,
        carpoolStart,
        carpoolEnd,
        rideWeight,
        carpoolFromPlace,
        carpoolToPlace,
        bookingInfo
      )
    );
    if (walkFromDropoff != null) {
      var walkEnd = carpoolEnd.plus(walkFromDropoff.duration());
      legs.add(buildWalkLeg(walkFromDropoff, carpoolEnd, walkEnd, dropoffPlace, itineraryEnd));
    }

    int totalCost = legs.stream().mapToInt(Leg::generalizedCost).sum();
    return Itinerary.ofDirect(legs).withGeneralizedCost(Cost.costOfSeconds(totalCost)).build();
  }

  private static CarpoolLeg buildCarpoolLeg(
    List<StreetPath> pathsWithNewPassenger,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    double rideWeight,
    Place fromPlace,
    Place toPlace,
    @Nullable BookingInfo bookingInfo
  ) {
    return CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(fromPlace)
      .withTo(toPlace)
      .withGeometry(
        GeometryUtils.concatenateLineStrings(pathsWithNewPassenger, StreetPath::geometry)
      )
      .withDistanceMeters(
        pathsWithNewPassenger.stream().mapToDouble(StreetPath::distanceMeters).sum()
      )
      .withGeneralizedCost((int) rideWeight)
      .withPickupBookingInfo(bookingInfo)
      .build();
  }

  private static StreetLeg buildWalkLeg(
    StreetPath walkPath,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    Place fromPlace,
    Place toPlace
  ) {
    return StreetLeg.of()
      .withMode(TraverseMode.WALK)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(fromPlace)
      .withTo(toPlace)
      .withGeometry(walkPath.geometry())
      .withDistanceMeters(walkPath.distanceMeters())
      .withGeneralizedCost((int) walkPath.weight())
      .build();
  }

  /**
   * The carpool leg's booking info from the trip's public contact: by phone if it has a phone
   * number, online if it has a booking URL. The URL is a {@link BookingUrlTemplate}, expanded with
   * where the passenger gets into ({@code pickup}) and out of ({@code dropoff}) the car.
   * {@code null} if the contact has neither.
   * <p>
   * {@code latestBookingTime} is a placeholder until a real booking deadline is sourced: the trip's
   * departure time of day at {@code daysPrior = 0}. It must be set, or the Transmodel API reports
   * the leg as not bookable in advance, see
   * {@code org.opentripplanner.apis.transmodel.mapping.BookingInfoMapper#mapToBookWhen}.
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
    String bookingUrl = contact.getBookingUrl();
    String expandedUrl =
      bookingUrl == null ? null : BookingUrlTemplate.expand(bookingUrl, pickup, dropoff);
    if (expandedUrl != null) {
      bookingMethods.add(BookingMethod.ONLINE);
    }
    ContactInfo effectiveContact = Objects.equals(expandedUrl, bookingUrl)
      ? contact
      : contact.copy().withBookingUrl(expandedUrl).build();

    return BookingInfo.of()
      .withContactInfo(effectiveContact)
      .withBookingMethods(bookingMethods)
      .withLatestBookingTime(new BookingTime(tripStartTime.toLocalTime(), 0))
      .build();
  }

  /**
   * The place of an outer endpoint of the itinerary: the label's transit stop, else its input
   * location, else the vertex. The vertex alone cannot name these: a stop's walk ends at a plain
   * street vertex, and without a walk the path starts at the snapped vertex, not at the user's
   * location.
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
   * The place of a vertex, named as the core street-leg mapper names it: a street intersection
   * after its streets, any other vertex, a {@link TemporaryStreetLocation} included, by its own
   * name.
   */
  private static Place makePlace(Vertex vertex) {
    if (vertex instanceof StreetVertex sv && !(vertex instanceof TemporaryStreetLocation)) {
      return Place.normal(vertex, sv.getIntersectionName());
    }
    return Place.normal(vertex, vertex.getName());
  }
}
