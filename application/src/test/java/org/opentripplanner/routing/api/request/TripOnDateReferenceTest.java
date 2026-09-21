package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
    var typed = assertInstanceOf(TripOnDateReferenceWithTripAndDate.class, ref);
    assertEquals(TRIP_ID, typed.id());
    assertEquals(SERVICE_DATE, typed.serviceDate());
  }

  @Test
  void createFromTripOnDateId() {
    var ref = TripOnDateReference.ofTripOnServiceDateId(TRIP_ON_DATE_ID);
    var typed = assertInstanceOf(TripOnDateReferenceWithTripOnServiceDateId.class, ref);
    assertEquals(TRIP_ON_DATE_ID, typed.id());
  }
}
