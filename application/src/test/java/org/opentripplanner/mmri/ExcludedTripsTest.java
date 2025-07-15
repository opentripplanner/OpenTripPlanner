package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class ExcludedTripsTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3e";
  }

  @Test
  public void test3e1() {
    Itinerary itinerary = plan(+1388530860L, "3e1", "3e2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388530980000L, 1388531040000L, "3e2", "3e1", null);

    assertEquals("Stop 3e1 ~ BUS bus 0:03 0:04 ~ Stop 3e2 [C₁90]", itinerary.toStr());
  }
}
