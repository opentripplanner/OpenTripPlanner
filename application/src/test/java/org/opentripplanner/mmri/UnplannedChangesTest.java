package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class UnplannedChangesTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3b";
  }

  @Test
  public void test3b1() {
    Itinerary itinerary = plan(+1388530860L, "3b1", "3b2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388531460000L, 1388531520000L, "3b2", "3b1", null);

    assertEquals("Stop 3b1 ~ BUS bus 0:11 0:12 ~ Stop 3b2 [C₁90]", itinerary.toStr());
  }

  @Test
  public void test3b2() {
    Itinerary itinerary = plan(+1388531460L, "3b1", "3b2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388531460000L, 1388531520000L, "3b2", "3b1", null);

    assertEquals("Stop 3b1 ~ BUS bus 0:11 0:12 ~ Stop 3b2 [C₁90]", itinerary.toStr());
  }
}
