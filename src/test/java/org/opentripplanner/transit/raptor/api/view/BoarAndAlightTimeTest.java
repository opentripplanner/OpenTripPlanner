package org.opentripplanner.transit.raptor.api.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class BoarAndAlightTimeTest {

  @Test
  public void testToString() {
    RaptorTripSchedule trip = TestTripSchedule
            .schedule("11:30 11:40 11:50")
            .pattern("L1", 2, 5, 3)
            .build();

    assertEquals(
            "[5 ~ 11:40 11:50(10m) ~ 3]",
            new BoardAndAlightTime(trip, 1, 2).toString()
    );
  }
}