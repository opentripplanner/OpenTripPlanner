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

  /**
   * Token based on the initial format, without stop ids.
   */
  private static final String ENCODED_TOKEN_V1 =
    "rO0ABXc2ABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjEABkY6VHJpcAAKMjAyMi0wMS0zMQAAAAEAAAAD";

  /**
   * Token based on the second version of the format, including stop ids.
   */
  private static final String ENCODED_TOKEN_V2 =
    "rO0ABXdZABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjIABkY6VHJpcAAKMjAyMi0wMS0zMQAAAAEAAAADAA9GOkJvYXJkaW5nIFN0b3AAEEY6QWxpZ2h0aW5nIFN0b3A=";

  /**
   * Token based on the latest format, including stop ids and TripOnServiceDate id.
   */
  private static final String ENCODED_TOKEN_V3 =
    "rO0ABXdbABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMABkY6VHJpcAAKMjAyMi0wMS0zMQAAAAEAAAADAA9GOkJvYXJkaW5nIFN0b3AAEEY6QWxpZ2h0aW5nIFN0b3AAAA==";

  @Test
  void testScheduledTransitLegReferenceRoundTrip() {
    var ref = new ScheduledTransitLegReference(
      TRIP_ID,
      SERVICE_DATE,
      FROM_STOP_POS,
      TO_STOP_POS,
      FROM_STOP_ID,
      TO_STOP_ID,
      null
    );

    var out = LegReferenceSerializer.encode(ref);

    assertEquals(ENCODED_TOKEN_V3, out);

    var ref2 = LegReferenceSerializer.decode(out);

    assertEquals(ref, ref2);
  }

  @Test
  void testScheduledTransitLegReferenceDeserialize() {
    var ref = (ScheduledTransitLegReference) LegReferenceSerializer.decode(ENCODED_TOKEN_V3);
    assertNotNull(ref);
    assertEquals(TRIP_ID, ref.tripId());
    assertEquals(SERVICE_DATE, ref.serviceDate());
    assertEquals(FROM_STOP_POS, ref.fromStopPositionInPattern());
    assertEquals(TO_STOP_POS, ref.toStopPositionInPattern());
  }

  @Test
  void testScheduledTransitLegReferenceLegacyV1Deserialize() {
    var ref = (ScheduledTransitLegReference) LegReferenceSerializer.decode(ENCODED_TOKEN_V1);
    assertNotNull(ref);
    assertEquals(TRIP_ID, ref.tripId());
    assertEquals(SERVICE_DATE, ref.serviceDate());
    assertEquals(FROM_STOP_POS, ref.fromStopPositionInPattern());
    assertEquals(TO_STOP_POS, ref.toStopPositionInPattern());
  }

  @Test
  void testScheduledTransitLegReferenceLegacyV2Deserialize() {
    var ref = (ScheduledTransitLegReference) LegReferenceSerializer.decode(ENCODED_TOKEN_V2);
    assertNotNull(ref);
    assertEquals(TRIP_ID, ref.tripId());
    assertEquals(SERVICE_DATE, ref.serviceDate());
    assertEquals(FROM_STOP_POS, ref.fromStopPositionInPattern());
    assertEquals(TO_STOP_POS, ref.toStopPositionInPattern());
  }
}
