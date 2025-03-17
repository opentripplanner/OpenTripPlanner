package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class OptimizationTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/2a1";
  }

  @Test
  public void test2a1() {
    Itinerary itinerary = plan(+1388530860L, "2a1", "2a2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388530860000L, 1388530920000L, "2a2", "2a1", null);

    assertEquals("Stop 2a1 ~ BUS short 0:01 0:02 ~ Stop 2a2 [C₁90]", itinerary.toStr());
  }

  @Test
  public void test2a2() {
    Itinerary itinerary = plan(+1388530980L, "2a1", "2a2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388531100000L, 1388531160000L, "2a2", "2a1", null);

    assertEquals("Stop 2a1 ~ BUS long 0:05 0:06 ~ Stop 2a2 [C₁90]", itinerary.toStr());
  }
}
