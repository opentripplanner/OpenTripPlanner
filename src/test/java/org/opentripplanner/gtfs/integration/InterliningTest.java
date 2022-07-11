package org.opentripplanner.gtfs.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;

public class InterliningTest extends GtfsTest {

  @Override
  public String getFeedName() {
    return "gtfs/interlining";
  }

  @Test
  public void testInterlining() {
    var zdt = ZonedDateTime.parse("2014-01-01T00:05:00-05:00[America/New_York]");
    long time = zdt.toEpochSecond();

    // We should arrive at the destination using two legs, both of which are on
    // the same route and with zero transfers.
    Itinerary itinerary = plan(time, "stop0", "stop3", null, false, false, null, null, null, 2);

    assertEquals(itinerary.getLegs().get(0).getRoute().getId().getId(), "route1");

    var secondLeg = itinerary.getLegs().get(1);
    assertEquals(secondLeg.getRoute().getId().getId(), "route1");
    assertTrue(secondLeg.isInterlinedWithPreviousLeg());
    assertEquals(0, itinerary.getNumberOfTransfers());
  }
}
