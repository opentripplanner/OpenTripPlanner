package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class LegReferenceSerializerTest {

  private static final FeedScopedId TRIP_ID = TransitModelForTest.id("Trip");
  private static final LocalDate SERVICE_DATE = LocalDate.of(2022, 1, 31);
  private static final int FROM_STOP_POS = 1;

  private static final FeedScopedId FROM_STOP_ID = TransitModelForTest.id("Boarding Stop");
  private static final int TO_STOP_POS = 3;
  private static final FeedScopedId TO_STOP_ID = TransitModelForTest.id("Alighting Stop");
  private static final String ENCODED_TOKEN =
    "rO0ABXdZABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjEABkY6VHJpcAAKMjAyMi0wMS0zMQAAAAEAAAADAA9GOkJvYXJkaW5nIFN0b3AAEEY6QWxpZ2h0aW5nIFN0b3A=";

  private static final String ENCODED_LEGACY_TOKEN =
    "rO0ABXc2ABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjEABkY6VHJpcAAKMjAyMi0wMS0zMQAAAAEAAAAD";

  @Test
  void testScheduledTransitLegReferenceRoundTrip() {
    var ref = new ScheduledTransitLegReference(
      TRIP_ID,
      SERVICE_DATE,
      FROM_STOP_POS,
      TO_STOP_POS,
      FROM_STOP_ID,
      TO_STOP_ID
    );

    var out = LegReferenceSerializer.encode(ref);

    assertEquals(ENCODED_TOKEN, out);

    var ref2 = LegReferenceSerializer.decode(out);

    assertEquals(ref, ref2);
  }

  @Test
  void testScheduledTransitLegReferenceDeserialize() {
    var ref = (ScheduledTransitLegReference) LegReferenceSerializer.decode(ENCODED_TOKEN);
    assertNotNull(ref);
    assertEquals(TRIP_ID, ref.tripId());
    assertEquals(SERVICE_DATE, ref.serviceDate());
    assertEquals(FROM_STOP_POS, ref.fromStopPositionInPattern());
    assertEquals(TO_STOP_POS, ref.toStopPositionInPattern());
  }

  @Test
  void testScheduledTransitLegReferenceLegacyDeserialize() {
    var ref = (ScheduledTransitLegReference) LegReferenceSerializer.decode(ENCODED_LEGACY_TOKEN);

    assertEquals(TRIP_ID, ref.tripId());
    assertEquals(SERVICE_DATE, ref.serviceDate());
    assertEquals(FROM_STOP_POS, ref.fromStopPositionInPattern());
    assertEquals(TO_STOP_POS, ref.toStopPositionInPattern());
  }
}
