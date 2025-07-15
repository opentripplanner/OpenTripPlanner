package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class FirstForbiddenTripToTripTransferTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/2e3";
  }

  @Test
  public void test2e3() {
    Itinerary itinerary = plan(+1388530860L, "2e31", "2e36", null, false, false, null, "", "", 2);

    Leg[] legs = itinerary.legs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530980000L, "2e34", "2e31", null);
    validateLeg(legs[1], 1388531040000L, 1388531100000L, "2e36", "2e34", null);

    assertEquals(
      "Stop 2e31 ~ RAIL train 1 0:01 0:03 ~ Stop 2e34 ~ RAIL train 2 0:04 0:05 ~ Stop 2e36 [C₁300]",
      itinerary.toStr()
    );
  }
}
