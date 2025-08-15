package org.opentripplanner.model.plan.leg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;

class ScheduledTransitLegTest {

  private static final ZonedDateTime START_TIME = OffsetDateTime.parse(
    "2023-04-17T17:49:06+02:00"
  ).toZonedDateTime();
  private static final ZonedDateTime END_TIME = START_TIME.plusMinutes(10);
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Route ROUTE = TimetableRepositoryForTest.route(id("2")).build();
  private static final TripPattern PATTERN = TimetableRepositoryForTest.tripPattern("1", ROUTE)
    .withStopPattern(TEST_MODEL.stopPattern(3))
    .build();
  private static final Trip TRIP = TimetableRepositoryForTest.trip("trip1").build();

  private static final ScheduledTripTimes TRIP_TIMES = ScheduledTripTimes.of()
    .withArrivalTimes("10:00 11:00 12:00")
    .withDepartureTimes("10:01 11:02 12:03")
    .withTrip(TRIP)
    .build();

  private static final int BOARD_STOP_INDEX_IN_PATTERN = 0;
  private static final int ALIGHT_STOP_INDEX_IN_PATTERN = 2;
  private static final int GENERALIZED_COST = 980;
  private static final double DISTANCE = 900.0;
  private static final ZoneId ZONE_ID = ZoneIds.BERLIN;
  private static final Duration DELAY = Duration.ofMinutes(4);
  private static final RealTimeTripTimes REAL_TIME_TRIP_TIMES =
    TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withDepartureTime(
        BOARD_STOP_INDEX_IN_PATTERN,
        TRIP_TIMES.getScheduledDepartureTime(BOARD_STOP_INDEX_IN_PATTERN) + (int) DELAY.toSeconds()
      )
      .build();

  private static final Set<TransitAlert> ALERTS = Set.of(
    TransitAlert.of(id("alert")).withDescriptionText(I18NString.of("alert")).build()
  );

  private static final Emission EMISSION = Emission.ofCo2Gram(23.0);
  private static final List<FareProduct> FARE_PRODUCTS = List.of(
    FareProduct.of(id("fp"), "fare product", Money.euros(10.00f)).build()
  );

  private final ScheduledTransitLeg subject = new ScheduledTransitLegBuilder()
    .withTripTimes(REAL_TIME_TRIP_TIMES)
    .withTripPattern(PATTERN)
    .withBoardStopIndexInPattern(BOARD_STOP_INDEX_IN_PATTERN)
    .withAlightStopIndexInPattern(ALIGHT_STOP_INDEX_IN_PATTERN)
    .withStartTime(START_TIME)
    .withEndTime(END_TIME)
    .withServiceDate(START_TIME.toLocalDate())
    .withZoneId(ZONE_ID)
    .withGeneralizedCost(GENERALIZED_COST)
    .withDistanceMeters(DISTANCE)
    .withAlerts(ALERTS)
    .withEmissionPerPerson(EMISSION)
    .withFareProducts(FARE_PRODUCTS)
    .build();

  @Test
  void testMinimalSetOfFieldsSet() {
    var subject = new ScheduledTransitLegBuilder()
      .withTripTimes(TRIP_TIMES)
      .withTripPattern(PATTERN)
      .withBoardStopIndexInPattern(BOARD_STOP_INDEX_IN_PATTERN)
      .withAlightStopIndexInPattern(ALIGHT_STOP_INDEX_IN_PATTERN)
      .withStartTime(START_TIME)
      .withEndTime(END_TIME)
      .withServiceDate(START_TIME.toLocalDate())
      .withZoneId(ZONE_ID)
      .withDistanceMeters(DISTANCE)
      .build();

    assertEquals(TRIP_TIMES, subject.tripTimes());
    assertEquals(PATTERN, subject.tripPattern());
    assertEquals(BOARD_STOP_INDEX_IN_PATTERN, subject.boardStopPosInPattern());
    assertEquals(ALIGHT_STOP_INDEX_IN_PATTERN, subject.alightStopPosInPattern());
    assertEquals(START_TIME, subject.startTime());
    assertEquals(END_TIME, subject.endTime());
    assertEquals(ZONE_ID, subject.zoneId());
    assertEquals(Cost.ZERO.toSeconds(), subject.generalizedCost());
    assertEquals(DISTANCE, subject.distanceMeters());

    // Uninitialized fields
    assertFalse(subject.isRealTimeUpdated());
    assertEquals(Set.of(), subject.listTransitAlerts());
    assertEquals(List.of(), subject.fareOffers());
  }

  @Test
  void testAccessors() {
    assertEquals(REAL_TIME_TRIP_TIMES, subject.tripTimes());
    assertEquals(PATTERN, subject.tripPattern());
    assertEquals(BOARD_STOP_INDEX_IN_PATTERN, subject.boardStopPosInPattern());
    assertEquals(ALIGHT_STOP_INDEX_IN_PATTERN, subject.alightStopPosInPattern());
    assertEquals(START_TIME, subject.startTime());
    assertEquals(END_TIME, subject.endTime());
    assertEquals(ZONE_ID, subject.zoneId());
    assertEquals(GENERALIZED_COST, subject.generalizedCost());
    assertEquals(DISTANCE, subject.distanceMeters());
    assertEquals(ALERTS, subject.listTransitAlerts());
    assertTrue(subject.isRealTimeUpdated());
    assertEquals(DELAY, subject.start().estimated().delay());
    assertNotNull(subject.end().estimated());
    assertEquals(EMISSION, subject.emissionPerPerson());
    assertEquals(FARE_PRODUCTS, subject.fareOffers());
  }

  @Test
  void testCopyOf() {
    // We need to change something(distance) because the copyOf() may return the same instance,
    // if not changed.
    var copy = subject.copyOf().withDistanceMeters(9.0).build();

    assertEquals(REAL_TIME_TRIP_TIMES, copy.tripTimes());
    assertEquals(PATTERN, copy.tripPattern());
    assertEquals(BOARD_STOP_INDEX_IN_PATTERN, copy.boardStopPosInPattern());
    assertEquals(ALIGHT_STOP_INDEX_IN_PATTERN, copy.alightStopPosInPattern());
    assertEquals(START_TIME, copy.startTime());
    assertEquals(END_TIME, copy.endTime());
    assertEquals(ZONE_ID, copy.zoneId());
    assertEquals(GENERALIZED_COST, copy.generalizedCost());
    assertEquals(ALERTS, copy.listTransitAlerts());
    assertEquals(EMISSION, copy.emissionPerPerson());
    assertEquals(FARE_PRODUCTS, copy.fareOffers());

    // We change something else, not distance and make sure distance is unchanged
    assertEquals(DISTANCE, subject.copyOf().withGeneralizedCost(9).build().distanceMeters());
  }

  @Test
  void testToString() {
    assertEquals(
      "ScheduledTransitLeg{" +
      "from: Place{name: Stop_0, stop: RegularStop{F:Stop_0 Stop_0}, coordinate: (60.0, 10.0), vertexType: TRANSIT}, " +
      "to: Place{name: Stop_2, stop: RegularStop{F:Stop_2 Stop_2}, coordinate: (60.0, 10.0), vertexType: TRANSIT}, " +
      "startTime: 2023-04-17T17:49:06, " +
      "endTime: 2023-04-17T17:59:06, " +
      "realTime: true, " +
      "distance: 900.0m, " +
      "generalizedCost: $980, " +
      "agencyId: F:A1, " +
      "routeId: F:Rtrip1, " +
      "tripId: F:trip1, " +
      "serviceDate: 2023-04-17, " +
      "boardRule: SCHEDULED, " +
      "alightRule: SCHEDULED, " +
      "transitAlerts: 1 items, " +
      "emissionPerPerson: Emission{CO₂: 23g}, " +
      "fareProducts: 1 items" +
      "}",
      subject.toString()
    );
  }
}
