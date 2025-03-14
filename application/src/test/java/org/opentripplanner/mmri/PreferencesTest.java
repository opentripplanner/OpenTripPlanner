package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class PreferencesTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/2c";
  }

  @Test
  public void test2c1() {
    Itinerary itinerary = plan(+1388530860L, "2c1", "2c3", null, false, false, null, "", "", 2);

    Leg[] legs = itinerary.legs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530920000L, "2c2", "2c1", null);
    validateLeg(legs[1], 1388530980000L, 1388531040000L, "2c3", "2c2", null);

    assertEquals(
      "Stop 2c1 ~ RAIL train 1 0:01 0:02 ~ Stop 2c2 ~ RAIL train 2 0:03 0:04 ~ Stop 2c3 [C₁240]",
      itinerary.toStr()
    );
  }
}
