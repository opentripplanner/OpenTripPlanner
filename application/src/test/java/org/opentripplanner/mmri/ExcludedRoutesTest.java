package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class ExcludedRoutesTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3d";
  }

  @Test
  public void test3d1() {
    Itinerary itinerary = plan(+1388530860L, "3d1", "3d2", null, false, false, null, "3d|1", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388530860000L, 1388530980000L, "3d2", "3d1", null);

    assertEquals("Stop 3d1 ~ BUS bus 2 0:01 0:03 ~ Stop 3d2 [C₁150]", itinerary.toStr());
  }
}
