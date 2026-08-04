package org.opentripplanner.ext.carpooling.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.ext.carpooling.CarpoolBookingUrlTestData.expectedAugmentedUrl;
import static org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder.createGraphPath;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.EndpointLabel;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.booking.BookingMethod;

/**
 * Unit tests for {@link CarpoolItineraryMapper}.
 * <p>
 * Two concerns are pinned here:
 * <ul>
 *   <li>{@link CarpoolItineraryMapper#toBookingInfo} — the pickup booking-info derivation from
 *       the trip's public contact details.</li>
 *   <li>Endpoint-naming precedence for itinerary boundaries:
 *     <ol>
 *       <li>Transit stop (Place#forStop) — when the chain ends at a transit stop</li>
 *       <li>User input location (Place#forGenericLocation) — the request's from/to label, with a
 *           localized "Origin"/"Destination" fallback when the label is null</li>
 *       <li>Vertex intersection name (StreetVertex#getIntersectionName) — for intermediate
 *           boundaries between walk and carpool legs</li>
 *     </ol>
 *   </li>
 * </ul>
 */
class CarpoolItineraryMapperTest {

  private static final ZonedDateTime TRIP_START = ZonedDateTime.of(
    2026,
    5,
    13,
    8,
    30,
    0,
    0,
    ZoneOffset.UTC
  );

  private static final WgsCoordinate PICKUP = new WgsCoordinate(59.910000, 10.750000);
  private static final WgsCoordinate DROPOFF = new WgsCoordinate(59.920000, 10.760000);

  private static final Duration STOP_DURATION = Duration.ofMinutes(2);
  private static final int PICKUP_POSITION = 1;
  private static final int DROPOFF_POSITION = 2;
  private static final Duration PICKUP_SEGMENT_DURATION = Duration.ofMinutes(1);
  private static final Duration SHARED_SEGMENT_DURATION = Duration.ofSeconds(60);
  private static final double CARPOOL_RELUCTANCE = 1.0;

  /**
   * Every GraphPath produced by {@link org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder}
   * has a single outgoing street edge named "segment-0", so the FIRST vertex of each path
   * (which carries that edge as outgoing) resolves to that name via
   * {@link org.opentripplanner.street.model.vertex.StreetVertex#getIntersectionName()}.
   */
  private static final String NAMED_INTERSECTION = "segment-0";

  /**
   * The LAST vertex of each test GraphPath has the segment edge only as <em>incoming</em> — there
   * are no outgoing street edges — so {@code getIntersectionName()} falls back to its
   * {@code LocalizedString("unnamedStreet")} branch, which resolves to "unnamed" in English. The
   * point of asserting this value is to prove it's a getIntersectionName() output (not the user's
   * "Office" label leaking through).
   */
  private static final String UNNAMED_INTERSECTION = "unnamed";

  private final CarpoolItineraryMapper mapper = new CarpoolItineraryMapper();
  private final TransitRepositoryForTest testModel = TransitRepositoryForTest.of();

  @Test
  void nullContact_returnsNull() {
    assertNull(CarpoolItineraryMapper.toBookingInfo(null, TRIP_START, PICKUP, DROPOFF));
  }

  /**
   * A contact that carries neither a phone number nor a booking URL has no actionable booking
   * method, so {@code toBookingInfo} must return {@code null} rather than a {@code BookingInfo}
   * with an empty {@code bookingMethods} set — otherwise the non-null return would
   * misleadingly suggest the leg is bookable. Consumers therefore may treat a non-null return
   * as "this leg has at least one booking method."
   */
  @Test
  void contactWithNeitherPhoneNorUrl_returnsNull() {
    var contact = ContactInfo.of().withContactPerson("Alice").build();

    assertNull(CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF));
  }

  @Test
  void phoneOnly_addsCallOfficeAndPreservesContactUnchanged() {
    var contact = ContactInfo.of().withPhoneNumber("+4712345678").build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    assertEquals(EnumSet.of(BookingMethod.CALL_OFFICE), info.bookingMethods());
    assertEquals(contact, info.getContactInfo());
  }

  @Test
  void urlOnly_addsOnlineAndAppendsPickupAndDropoffCoordinatesToUrl() {
    var contact = ContactInfo.of().withBookingUrl("https://book.example.com").build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    assertEquals(EnumSet.of(BookingMethod.ONLINE), info.bookingMethods());
    assertEquals(
      expectedAugmentedUrl("https://book.example.com", PICKUP, DROPOFF),
      info.getContactInfo().getBookingUrl()
    );
  }

  /**
   * Guards against the easy bug of unconditionally appending {@code ?from_coordinate=…}, which
   * would yield an invalid double-{@code ?} URL when the provider's booking URL already carries
   * its own query string (e.g. tracking parameters). The coordinate parameters must be appended
   * with {@code &} in that case.
   */
  @Test
  void urlWithExistingQueryString_usesAmpersandSeparator() {
    var contact = ContactInfo.of().withBookingUrl("https://book.example.com/?ref=foo").build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    var expected = String.format(
      Locale.ROOT,
      "https://book.example.com/?ref=foo&from_coordinate=%.6f,%.6f&to_coordinate=%.6f,%.6f",
      PICKUP.latitude(),
      PICKUP.longitude(),
      DROPOFF.latitude(),
      DROPOFF.longitude()
    );
    assertEquals(expected, info.getContactInfo().getBookingUrl());
  }

  @Test
  void phoneAndUrl_addsBothMethodsAndOnlyRewritesUrl() {
    var contact = ContactInfo.of()
      .withPhoneNumber("+4712345678")
      .withBookingUrl("https://book.example.com")
      .build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    assertEquals(
      EnumSet.of(BookingMethod.CALL_OFFICE, BookingMethod.ONLINE),
      info.bookingMethods()
    );
    var augmented = info.getContactInfo();
    assertEquals("+4712345678", augmented.getPhoneNumber());
    assertEquals(
      expectedAugmentedUrl("https://book.example.com", PICKUP, DROPOFF),
      augmented.getBookingUrl()
    );
  }

  /**
   * Pins down the URL with a {@code #fragment}: the appended coordinate parameters must end up
   * in the query (before the fragment), not inside the fragment. The string-level
   * {@code url.contains("?")} predicate doesn't catch this — switching to {@link java.net.URI}
   * parsing does, because the fragment is split off the URL before it can swallow the params.
   */
  @Test
  void urlWithFragment_appendsParamsBeforeFragment() {
    var contact = ContactInfo.of().withBookingUrl("https://book.example.com/page#bookform").build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    var expected = String.format(
      Locale.ROOT,
      "https://book.example.com/page?from_coordinate=%.6f,%.6f&to_coordinate=%.6f,%.6f#bookform",
      PICKUP.latitude(),
      PICKUP.longitude(),
      DROPOFF.latitude(),
      DROPOFF.longitude()
    );
    assertEquals(expected, info.getContactInfo().getBookingUrl());
  }

  /**
   * When the booking URL is unparseable, the URL is dropped from the contact and
   * {@link BookingMethod#ONLINE} is removed from the booking methods rather than left in place
   * pointing nowhere. If the contact carried only the (now-dropped) URL, no actionable booking
   * method remains and {@code toBookingInfo} returns {@code null}, matching the
   * "non-null return ⇒ at least one usable method" contract.
   */
  @Test
  void malformedUrlOnly_returnsNull() {
    var contact = ContactInfo.of().withBookingUrl("https://book.example.com/has space").build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNull(info);
  }

  /**
   * Same malformed-URL handling as {@link #malformedUrlOnly_returnsNull()} but with a phone
   * number also present: the call-office method survives, the dropped URL is reflected as
   * {@code null} on the returned contact, and {@code ONLINE} is absent from the booking methods.
   */
  @Test
  void malformedUrlWithPhone_keepsCallOfficeAndDropsUrl() {
    var contact = ContactInfo.of()
      .withPhoneNumber("+4712345678")
      .withBookingUrl("https://book.example.com/has space")
      .build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    assertEquals(EnumSet.of(BookingMethod.CALL_OFFICE), info.bookingMethods());
    assertEquals("+4712345678", info.getContactInfo().getPhoneNumber());
    assertNull(info.getContactInfo().getBookingUrl());
  }

  /**
   * Pins down the placeholder behaviour documented on {@code toBookingInfo}: {@code
   * latestBookingTime} must be non-null with {@code daysPrior == 0} and the time-of-day taken
   * from the driver's trip start. This shape is load-bearing for the Transmodel
   * {@code BookingArrangement.bookWhen} mapping, which collapses to {@code "timeOfTravelOnly"}
   * if {@code latestBookingTime} is null.
   */
  @Test
  void latestBookingTime_isTripStartTimeOfDayAtDaysPriorZero() {
    var contact = ContactInfo.of().withPhoneNumber("+4712345678").build();

    var info = CarpoolItineraryMapper.toBookingInfo(contact, TRIP_START, PICKUP, DROPOFF);

    assertNotNull(info);
    var latest = info.getLatestBookingTime();
    assertNotNull(latest);
    assertEquals(LocalTime.of(8, 30), latest.getTime());
    assertEquals(0, latest.getDaysPrior());
  }

  /**
   * Tier 2 — outer endpoints carry the user's from/to label when present. Without a transit
   * stop in play, the labels passed via {@code fromLocation}/{@code toLocation} reach the first
   * leg's {@code from} and the last leg's {@code to} directly.
   */
  @Test
  void outerEndpointsUseUserSuppliedLabels() {
    var candidate = newCandidate(
      createGraphPath(Duration.ofSeconds(80)),
      createGraphPath(Duration.ofSeconds(40))
    );

    var itinerary = mapper.toItinerary(
      candidate,
      CARPOOL_RELUCTANCE,
      GenericLocation.fromCoordinate(59.91, 10.74, "Home"),
      GenericLocation.fromCoordinate(59.95, 10.80, "Office")
    );

    var legs = itinerary.legs();
    assertEquals("Home", legs.getFirst().from().name.toString());
    assertEquals("Office", legs.getLast().to().name.toString());
  }

  /**
   * Tier 2 fallback — when the GenericLocation has no label (coordinate-only request) the mapper
   * falls back to the "Origin"/"Destination" strings.
   */
  @Test
  void outerEndpointsFallBackToLocalizedOriginDestination() {
    var candidate = newCandidate(
      createGraphPath(Duration.ofSeconds(80)),
      createGraphPath(Duration.ofSeconds(40))
    );

    var itinerary = mapper.toItinerary(
      candidate,
      CARPOOL_RELUCTANCE,
      GenericLocation.fromCoordinate(59.91, 10.74, null),
      GenericLocation.fromCoordinate(59.95, 10.80, null)
    );

    var legs = itinerary.legs();
    assertEquals("Origin", legs.getFirst().from().name.toString());
    assertEquals("Destination", legs.getLast().to().name.toString());
  }

  /**
   * Tier 2 fallback — same as the null-label case but for the empty-string label that
   * {@link org.opentripplanner.apis.transmodel.mapping.GenericLocationMapper} produces when the
   * GraphQL request omits {@code name}.
   */
  @Test
  void outerEndpointsFallBackWhenLabelIsEmptyString() {
    var candidate = newCandidate(
      createGraphPath(Duration.ofSeconds(80)),
      createGraphPath(Duration.ofSeconds(40))
    );

    var itinerary = mapper.toItinerary(
      candidate,
      CARPOOL_RELUCTANCE,
      GenericLocation.fromCoordinate(59.91, 10.74, ""),
      GenericLocation.fromCoordinate(59.95, 10.80, "")
    );

    var legs = itinerary.legs();
    assertEquals("Origin", legs.getFirst().from().name.toString());
    assertEquals("Destination", legs.getLast().to().name.toString());
  }

  /**
   * Tier 3 — intermediate boundaries (between a walk leg and the carpool leg) are
   * vertex-derived via {@link
   * org.opentripplanner.street.model.vertex.StreetVertex#getIntersectionName()} and must NOT
   * inherit the user's from/to label. This is what keeps the carpool leg's pickup/dropoff
   * looking like actual street locations rather than the rider's labelled origin/destination.
   */
  @Test
  void carpoolBoundaryUsesIntersectionNameNotUserLabel() {
    var candidate = newCandidate(
      createGraphPath(Duration.ofSeconds(80)),
      createGraphPath(Duration.ofSeconds(40))
    );

    var itinerary = mapper.toItinerary(
      candidate,
      CARPOOL_RELUCTANCE,
      GenericLocation.fromCoordinate(59.91, 10.74, "Home"),
      GenericLocation.fromCoordinate(59.95, 10.80, "Office")
    );

    var legs = itinerary.legs();
    // Walk-to-pickup's TO == carpool's FROM == pickup vertex.
    // The pickup vertex is the FIRST vertex of the shared GraphPath and carries the segment edge
    // as outgoing, so getIntersectionName() returns the edge name "segment-0".
    assertEquals(NAMED_INTERSECTION, legs.getFirst().to().name.toString());
    assertEquals(NAMED_INTERSECTION, legs.get(1).from().name.toString());
    // Carpool's TO == walk-from-dropoff's FROM == dropoff vertex.
    // The dropoff vertex is the LAST vertex of the shared GraphPath and has no outgoing street
    // edges, so getIntersectionName() falls back to "unnamed". Crucially, this is NOT the user's
    // "Office" label — that's the property under test.
    assertEquals(UNNAMED_INTERSECTION, legs.get(1).to().name.toString());
    assertEquals(UNNAMED_INTERSECTION, legs.getLast().from().name.toString());
  }

  /**
   * Tier 1 — for an access itinerary (passenger origin → walk → carpool → transit stop), the
   * carpool leg's {@code to} (which is the last leg, since no walk-from-dropoff is needed when
   * the carpool ends at the stop) carries the transit stop's name. Transit stop wins over both
   * the intersection name and any user-supplied destination label.
   * <p>
   * The first leg's {@code from} simultaneously verifies that the passenger origin label is
   * still applied on the user-side end of an access chain.
   */
  @Test
  void accessItineraryLastLegToCarriesTransitStopName() {
    RegularStop stop = testModel.stop("Central Station", 59.91, 10.74).build();
    GenericLocation passengerOrigin = GenericLocation.fromCoordinate(59.92, 10.75, "Home");
    var candidate = newCandidate(
      createGraphPath(Duration.ofSeconds(80)),
      // Access: the carpool ends at the transit stop, no walk-from-dropoff.
      null
    );
    var accessEgress = new CarpoolAccessEgress(
      0,
      0,
      candidate,
      TimeAndCost.ZERO,
      CARPOOL_RELUCTANCE,
      EndpointLabel.forLocation(passengerOrigin),
      EndpointLabel.forStop(stop)
    );

    var itinerary = mapper.toItinerary(accessEgress);

    var legs = itinerary.legs();
    assertEquals("Home", legs.getFirst().from().name.toString());
    assertEquals("Central Station", legs.getLast().to().name.toString());
  }

  /**
   * Tier 1 symmetric to {@link #accessItineraryLastLegToCarriesTransitStopName()} — for an egress
   * itinerary (transit stop → carpool → walk → passenger destination), the carpool leg's
   * {@code from} (the first leg, since no walk-to-pickup is needed when the carpool starts at the
   * stop) carries the transit stop's name. The last walk leg's {@code to} simultaneously verifies
   * that the passenger destination label is still applied on the user-side end of an egress chain.
   */
  @Test
  void egressItineraryFirstLegFromCarriesTransitStopName() {
    RegularStop stop = testModel.stop("Central Station", 59.91, 10.74).build();
    GenericLocation passengerDestination = GenericLocation.fromCoordinate(59.92, 10.75, "Office");
    var candidate = newCandidate(
      // Egress: the carpool starts at the transit stop, no walk-to-pickup.
      null,
      createGraphPath(Duration.ofSeconds(40))
    );
    var accessEgress = new CarpoolAccessEgress(
      0,
      0,
      candidate,
      TimeAndCost.ZERO,
      CARPOOL_RELUCTANCE,
      EndpointLabel.forStop(stop),
      EndpointLabel.forLocation(passengerDestination)
    );

    var itinerary = mapper.toItinerary(accessEgress);

    var legs = itinerary.legs();
    assertEquals("Central Station", legs.getFirst().from().name.toString());
    assertEquals("Office", legs.getLast().to().name.toString());
  }

  private InsertionCandidate newCandidate(
    @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
    @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff
  ) {
    return new InsertionCandidate(
      createSimpleTrip(OSLO_CENTER, OSLO_NORTH),
      PICKUP_POSITION,
      DROPOFF_POSITION,
      List.of(createGraphPath(PICKUP_SEGMENT_DURATION), createGraphPath(SHARED_SEGMENT_DURATION)),
      STOP_DURATION,
      null,
      walkToPickup,
      walkFromDropoff
    );
  }
}
