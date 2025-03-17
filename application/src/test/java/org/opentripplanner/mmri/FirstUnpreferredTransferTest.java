package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class FirstUnpreferredTransferTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3g1";
  }

  @Test
  public void test3g1() {
    Itinerary itinerary = plan(+1388530860L, "3g11", "3g16", null, false, false, null, "", "", 2);

    Leg[] legs = itinerary.legs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530980000L, "3g14", "3g11", null);
    validateLeg(legs[1], 1388531040000L, 1388531100000L, "3g16", "3g14", null);

    assertEquals(
      "Stop 3g11 ~ RAIL train 1 0:01 0:03 ~ Stop 3g14 ~ RAIL train 2 0:04 0:05 ~ Stop 3g16 [C₁300]",
      itinerary.toStr()
    );
  }
}
