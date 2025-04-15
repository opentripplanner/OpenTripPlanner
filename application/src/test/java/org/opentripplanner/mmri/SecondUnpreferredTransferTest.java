package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class SecondUnpreferredTransferTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3g2";
  }

  @Test
  public void test3g2() {
    Itinerary itinerary = plan(+1388530860L, "3g21", "3g26", null, false, false, null, "", "", 2);

    Leg[] legs = itinerary.legs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530920000L, "3g23", "3g21", null);
    validateLeg(legs[1], 1388530980000L, 1388531100000L, "3g26", "3g23", null);

    assertEquals(
      "Stop 3g21 ~ RAIL train 1 0:01 0:02 ~ Stop 3g23 ~ RAIL train 2 0:03 0:05 ~ Stop 3g26 [C₁300]",
      itinerary.toStr()
    );
  }
}
