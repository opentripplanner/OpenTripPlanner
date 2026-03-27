package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class TripOnDateReferenceTest {

  private static final FeedScopedId TRIP_ID = new FeedScopedId("F", "trip1");
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 1);
  private static final FeedScopedId TRIP_ON_DATE_ID = new FeedScopedId("F", "dated-trip1");

  @Test
  void createFromTripIdAndServiceDate() {
    var ref = TripOnDateReference.ofTripIdAndServiceDate(TRIP_ID, SERVICE_DATE);
    assertNotNull(ref.tripIdOnServiceDate());
    assertEquals(TRIP_ID, ref.tripIdOnServiceDate().tripId());
    assertEquals(SERVICE_DATE, ref.tripIdOnServiceDate().serviceDate());
    assertNull(ref.tripOnServiceDateId());
  }

  @Test
  void createFromTripOnDateId() {
    var ref = TripOnDateReference.ofTripOnServiceDateId(TRIP_ON_DATE_ID);
    assertNull(ref.tripIdOnServiceDate());
    assertEquals(TRIP_ON_DATE_ID, ref.tripOnServiceDateId());
  }

  @Test
  void throwsWhenBothNull() {
    assertThrows(IllegalArgumentException.class, () -> new TripOnDateReference(null, null));
  }

  @Test
  void throwsWhenBothSet() {
    assertThrows(IllegalArgumentException.class, () ->
      new TripOnDateReference(
        new org.opentripplanner.transit.model.timetable.TripIdAndServiceDate(TRIP_ID, SERVICE_DATE),
        TRIP_ON_DATE_ID
      )
    );
  }
}
