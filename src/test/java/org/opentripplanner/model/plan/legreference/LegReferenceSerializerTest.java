package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;

class LegReferenceSerializerTest {

  private static final FeedScopedId TRIP_ID = new FeedScopedId("F", "Trip");
  private static final ServiceDate SERVICE_DATE = new ServiceDate(2022, 1, 31);
  private static final int FROM_STOP_POS = 1;
  private static final int TO_STOP_POS = 3;

  @Test
  void testScheduledTransitLegReferenceRoundTrip() {
    var ref = new ScheduledTransitLegReference(TRIP_ID, SERVICE_DATE, 1, 3);

    var out = LegReferenceSerializer.encode(ref);

    System.out.println(out);

    var ref2 = LegReferenceSerializer.decode(out);

    assertEquals(ref, ref2);
  }

  @Test
  void testScheduledTransitLegReferenceDeserialize() {
    var in = "rO0ABXc0ABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjEABkY6VHJpcAAIMjAyMjAxMzEAAAABAAAAAw==";

    var ref = (ScheduledTransitLegReference) LegReferenceSerializer.decode(in);

    assertEquals(TRIP_ID, ref.tripId());
    assertEquals(SERVICE_DATE, ref.serviceDate());
    assertEquals(FROM_STOP_POS, ref.fromStopPositionInPattern());
    assertEquals(TO_STOP_POS, ref.toStopPositionInPattern());
  }
}
