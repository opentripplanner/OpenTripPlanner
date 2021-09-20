package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class BoarAndAlightTimeTest {

  @Test
  public void testToString() {
    RaptorTripSchedule trip = TestTripSchedule.schedule("11:30, 11:40, 11:50").build();

    assertEquals("(11:40, 11:50)", new BoarAndAlightTime(trip, 1, 2).toString());
  }
}