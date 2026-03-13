package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTripWithTime;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTripWithTimes;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeBasedFilterTest {
  private final TimeBasedFilter filter = new TimeBasedFilter();
  private static final boolean ARRIVE_BY = true;
  private static final boolean DEPART_AFTER = false;

  @Test
  void accepts_arriveByPassengerRequestWithinTimeWindow_returnsTrue() {
    var startTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    // Trip departs at 11:15
    var endTime = ZonedDateTime.parse("2024-01-15T11:15:00+01:00");
    var trip = createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, startTime, endTime);

    // Passenger requests arrive by at 11:30 (15 minutes after trip departure)
    var passengerRequestTime = endTime.plusMinutes(15).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(ARRIVE_BY)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_arriveByPassengerRequestExactlyAtTripArrival_returnsTrue() {
    var startTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    // Trip departs at 11:15
    var endTime = ZonedDateTime.parse("2024-01-15T11:15:00+01:00");
    var trip = createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, startTime, endTime);

    // Passenger requests exactly when trip arrives
    var passengerRequestTime = endTime.toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(ARRIVE_BY)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_arriveByPassengerRequestAtWindowBoundary_returnsTrue() {
    var startTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    // Trip departs at 11:15
    var endTime = ZonedDateTime.parse("2024-01-15T11:15:00+01:00");
    var trip = createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, startTime, endTime);

    // Passenger requests exactly when trip arrives
    var passengerRequestTime = endTime.plusMinutes(30).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(ARRIVE_BY)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestWithinTimeWindow_returnsTrue() {
    // Trip departs at 10:00
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests at 10:15 (15 minutes after trip departure)
    var passengerRequestTime = tripDepartureTime.plusMinutes(15).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(false)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestExactlyAtTripDeparture_returnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests exactly when trip departs
    var passengerRequestTime = tripDepartureTime.toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestAtWindowBoundary_returnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests exactly 30 minutes after (at boundary)
    var passengerRequestTime = tripDepartureTime.plusMinutes(30).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestBeforeTripDepartureWithinWindow_returnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 20 minutes before trip departs (within 30-min window)
    var passengerRequestTime = tripDepartureTime.minusMinutes(20).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertTrue(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestTooFarInFuture_returnsFalse() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 45 minutes after trip departs (outside 30-min window)
    var passengerRequestTime = tripDepartureTime.plusMinutes(45).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertFalse(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestTooFarInPast_returnsFalse() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 45 minutes before trip departs (outside 30-min window)
    var passengerRequestTime = tripDepartureTime.minusMinutes(45).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertFalse(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_departAfterPassengerRequestWayTooLate_returnsFalse() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);

    // Passenger requests 2 hours after trip departs
    var passengerRequestTime = tripDepartureTime.plusHours(2).toInstant();
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .withRequestedDateTime(passengerRequestTime)
      .build();

    assertFalse(filter.accepts(trip, request, Duration.ofMinutes(30)));
  }

  @Test
  void accepts_withoutTimeParameter_alwaysReturnsTrue() {
    var tripDepartureTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
    var trip = createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, tripDepartureTime);
    var request = new CarpoolingRequestBuilder()
      .withIsArriveByRequest(DEPART_AFTER)
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_NORTHEAST)
      .build();

    // When called without time parameter, should accept (with warning log)
    assertTrue(filter.accepts(trip, request, null));
  }
}
