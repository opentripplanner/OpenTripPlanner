package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;

@Disabled
public class PatternInterlineDwellTest extends GtfsTest {

  @Override
  public String getFeedName() {
    return "gtfs/interlining";
  }

  // TODO Allow using Calendar or ISOdate for testing, interpret it in the given graph's timezone.

  @Test
  public void testInterlining() {
    Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 01, 00, 05, 00);
    calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    long time = calendar.getTime().getTime() / 1000;
    // We should arrive at the destination using two legs, both of which are on
    // the same route and with zero transfers.
    Itinerary itinerary = plan(time, "stop0", "stop3", null, false, false, null, null, null, 2);

    assertEquals(itinerary.legs.get(0).getRoute().getId().getId(), "route1");
    assertEquals(itinerary.legs.get(1).getRoute().getId().getId(), "route1");
    assertEquals(0, itinerary.nTransfers);
  }
  // TODO test for trips on the same block with no transfer allowed (Trimet special case)

}
