package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class BeneficialChangesTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3c";
  }

  @Test
  public void test3c1() {
    Itinerary itinerary = plan(+1388531040L, "3c2", "3c3", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388531040000L, 1388531100000L, "3c3", "3c2", null);

    assertEquals("Stop 3c2 ~ BUS bus 0:04 0:05 ~ Stop 3c3 [C₁90]", itinerary.toStr());
  }
}
