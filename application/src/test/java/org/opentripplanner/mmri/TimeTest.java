package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class TimeTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/1g";
  }

  @Test
  public void test1g1() {
    Itinerary itinerary = plan(+1388530920L, "1g1", "1g2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388530980000L, 1388531040000L, "1g2", "1g1", null);

    assertEquals("Stop 1g1 ~ BUS bus 0:03 0:04 ~ Stop 1g2 [C₁90]", itinerary.toStr());
  }

  @Test
  public void test1g2() {
    Itinerary itinerary = plan(-1388530980L, "1g1", "1g2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388530860000L, 1388530920000L, "1g2", "1g1", null);

    assertEquals("Stop 1g1 ~ BUS bus 0:01 0:02 ~ Stop 1g2 [C₁90]", itinerary.toStr());
  }

  @Test
  public void test1g5() {
    Itinerary itinerary = plan(+1388703780L, "1g1", "1g2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388703780000L, 1388703840000L, "1g2", "1g1", null);

    assertEquals("Stop 1g1 ~ BUS bus 0:03 0:04 ~ Stop 1g2 [C₁90]", itinerary.toStr());
  }

  @Test
  public void test1g6() {
    Itinerary itinerary = plan(-1388703840L, "1g1", "1g2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs().toArray(new Leg[1])[0];

    validateLeg(leg, 1388703660000L, 1388703720000L, "1g2", "1g1", null);

    assertEquals("Stop 1g1 ~ BUS bus 0:01 0:02 ~ Stop 1g2 [C₁90]", itinerary.toStr());
  }
}
