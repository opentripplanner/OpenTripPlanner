package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class StopToStopTransfersTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/2d";
  }

  @Test
  public void test2d1() {
    Itinerary itinerary = plan(+1388530860L, "2d1", "2d4", null, false, false, null, "", "", 2);

    Leg[] legs = itinerary.legs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530980000L, "2d3", "2d1", null);
    validateLeg(legs[1], 1388530980000L, 1388531040000L, "2d4", "2d3", null);

    assertEquals(
      "Stop 2d1 ~ RAIL train 1 0:01 0:03 ~ Stop 2d3 ~ RAIL train 2 0:03 0:04 ~ Stop 2d4 [C₁210]",
      itinerary.toStr()
    );
  }
}
