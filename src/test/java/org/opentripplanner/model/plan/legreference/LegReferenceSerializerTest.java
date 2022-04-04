package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;

class LegReferenceSerializerTest {

  @Test
  void testScheduledTransitLegReferenceRoundTrip() {
    var ref = new ScheduledTransitLegReference(
      new FeedScopedId("Feed", "Trip"),
      new ServiceDate(2022, 1, 31),
      1,
      3
    );

    var out = LegReferenceSerializer.encode(ref);

    var ref2 = LegReferenceSerializer.decode(out);

    assertEquals(ref, ref2);
  }
}
