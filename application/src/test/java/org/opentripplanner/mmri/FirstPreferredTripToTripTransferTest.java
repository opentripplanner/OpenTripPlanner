package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class FirstPreferredTripToTripTransferTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/2e1";
  }

  @Test
  public void test2e1() {
    Itinerary itinerary = plan(+1388530860L, "2e11", "2e16", null, false, false, null, "", "", 2);

    Leg[] legs = itinerary.legs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530920000L, "2e13", "2e11", null);
    validateLeg(legs[1], 1388530980000L, 1388531100000L, "2e16", "2e13", null);

    assertEquals(
      "Stop 2e11 ~ RAIL train 1 0:01 0:02 ~ Stop 2e13 ~ RAIL train 2 0:03 0:05 ~ Stop 2e16 [C₁270]",
      itinerary.toStr()
    );
  }
}
