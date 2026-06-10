package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class TripLocationTest {

  private static final FeedScopedId TRIP_ID = new FeedScopedId("F", "trip1");
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 1);
  private static final FeedScopedId STOP_ID = new FeedScopedId("F", "stop1");
  private static final TripOnDateReference TRIP_REF = TripOnDateReference.ofTripIdAndServiceDate(
    TRIP_ID,
    SERVICE_DATE
  );

  @Test
  void createWithStopId() {
    var tripLocation = TripLocation.of(TRIP_REF, STOP_ID);
    assertEquals(TRIP_REF, tripLocation.tripOnDateReference());
    assertEquals(STOP_ID, tripLocation.stopLocationId());
    assertNull(tripLocation.aimedDepartureTime());
  }

  @Test
  void createWithStopIdAndAimedDepartureTime() {
    var instant = Instant.ofEpochMilli(1_730_000_000_000L);
    var tripLocation = TripLocation.of(TRIP_REF, STOP_ID, instant);
    assertEquals(TRIP_REF, tripLocation.tripOnDateReference());
    assertEquals(STOP_ID, tripLocation.stopLocationId());
    assertEquals(instant, tripLocation.aimedDepartureTime());
  }

  @Test
  void throwsWhenTripReferenceIsNull() {
    assertThrows(NullPointerException.class, () -> TripLocation.of(null, STOP_ID));
  }

  @Test
  void throwsWhenStopIdIsNull() {
    assertThrows(NullPointerException.class, () -> TripLocation.of(TRIP_REF, null));
  }
}
