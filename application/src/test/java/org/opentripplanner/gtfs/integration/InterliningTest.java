package org.opentripplanner.gtfs.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;

public class InterliningTest extends GtfsTest {

  long time = ZonedDateTime.parse("2014-01-01T00:05:00-05:00[America/New_York]").toEpochSecond();

  @Override
  public String getFeedName() {
    return "gtfs/interlining";
  }

  @Test
  public void interlineOnSameRoute() {
    // We should arrive at the destination using two legs, both of which are on
    // the same route and with zero transfers.
    Itinerary itinerary = plan(time, "stop0", "stop3", null, false, false, null, null, null, 2);

    assertEquals(itinerary.legs().get(0).route().getId().getId(), "route1");

    var secondLeg = itinerary.legs().get(1);
    assertEquals(secondLeg.route().getId().getId(), "route1");
    assertTrue(secondLeg.isInterlinedWithPreviousLeg());
    assertEquals(0, itinerary.numberOfTransfers());
  }

  @Test
  public void interlineOnDifferentRoute() {
    var itinerary = plan(time, "stop0", "stop6", null, false, false, null, null, null, 2);

    assertEquals(itinerary.legs().get(0).route().getId().getId(), "route0");

    var secondLeg = itinerary.legs().get(1);
    assertEquals(secondLeg.route().getId().getId(), "route3");
    assertTrue(secondLeg.isInterlinedWithPreviousLeg());
    assertEquals(0, itinerary.numberOfTransfers());
  }

  @Test
  public void staySeatedNotAllowed() {
    var itinerary = plan(time, "stop0", "stop5", null, false, false, null, null, null, 2);

    assertEquals(itinerary.legs().get(0).route().getId().getId(), "route2");

    var secondLeg = itinerary.legs().get(1);
    assertEquals(secondLeg.route().getId().getId(), "route2");
    assertFalse(secondLeg.isInterlinedWithPreviousLeg());
    assertEquals(1, itinerary.numberOfTransfers());
  }
}
