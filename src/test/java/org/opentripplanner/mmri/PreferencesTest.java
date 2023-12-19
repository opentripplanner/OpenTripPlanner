package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;

import org.junit.jupiter.api.Disabled;
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

    Leg[] legs = itinerary.getLegs().toArray(new Leg[2]);

    validateLeg(legs[0], 1388530860000L, 1388530920000L, "2c2", "2c1", null);
    validateLeg(legs[1], 1388530980000L, 1388531040000L, "2c3", "2c2", null);

    assertEquals(
      "Stop 2c1 ~ RAIL train 1 0:01 0:02 ~ Stop 2c2 ~ RAIL train 2 0:03 0:04 ~ Stop 2c3 [C₁240]",
      itinerary.toStr()
    );
  }

  @Test
  @Disabled
  public void test2c2() {
    Itinerary itinerary = plan(+1388530860L, "2c1", "2c3", null, false, false, BUS, "", "", 3);

    Leg[] legs = itinerary.getLegs().toArray(new Leg[3]);

    validateLeg(legs[1], 1388530920000L, 1388531160000L, "2c5", "2c4", null);

    assertEquals("", itinerary.toStr());
  }

  @Test
  @Disabled
  public void test2c3() {
    Itinerary itinerary = plan(+1388530860L, "2c1", "2c3", null, false, true, null, "", "", 3);

    Leg[] legs = itinerary.getLegs().toArray(new Leg[3]);

    validateLeg(legs[1], 1388530920000L, 1388531160000L, "2c5", "2c4", null);

    assertEquals("", itinerary.toStr());
  }
}
