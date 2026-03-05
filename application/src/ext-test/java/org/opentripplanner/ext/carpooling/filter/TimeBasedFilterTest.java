package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTripWithTime;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeBasedFilterTest {

  private TimeBasedFilter filter;

  @BeforeEach
  void setup() {
    filter = new TimeBasedFilter();
  }

  @Test
  void accepts_passengerRequestWithinTimeWindow_returnsTrue() {
    // Trip departs at 10:00
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests at 10:15 (15 minutes after trip departure)
    var passengerRequestTime = tripDepartureTime.plusMinutes(15).toInstant();

    assertTrue(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void accepts_passengerRequestExactlyAtTripDeparture_returnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests exactly when trip departs
    var passengerRequestTime = tripDepartureTime.toInstant();

    assertTrue(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void accepts_passengerRequestAtWindowBoundary_returnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests exactly 30 minutes after (at boundary)
    var passengerRequestTime = tripDepartureTime.plusMinutes(30).toInstant();

    assertTrue(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void accepts_passengerRequestBeforeTripDeparture_withinWindow_returnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 20 minutes before trip departs (within 30-min window)
    var passengerRequestTime = tripDepartureTime.minusMinutes(20).toInstant();

    assertTrue(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void rejects_passengerRequestTooFarInFuture_returnsFalse() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 45 minutes after trip departs (outside 30-min window)
    var passengerRequestTime = tripDepartureTime.plusMinutes(45).toInstant();

    assertFalse(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void rejects_passengerRequestTooFarInPast_returnsFalse() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 45 minutes before trip departs (outside 30-min window)
    var passengerRequestTime = tripDepartureTime.minusMinutes(45).toInstant();

    assertFalse(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void rejects_passengerRequestWayTooLate_returnsFalse() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 2 hours after trip departs
    var passengerRequestTime = tripDepartureTime.plusHours(2).toInstant();

    assertFalse(
      filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST, passengerRequestTime, Duration.ofMinutes(30))
    );
  }

  @Test
  void acceptsWithoutTimeParameter_alwaysReturnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // When called without time parameter, should accept (with warning log)
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_NORTHEAST));
  }
}
