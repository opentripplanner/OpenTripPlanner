package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;

public class WheelchairTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/2b";
  }

  @Test
  public void test2b1() {
    Itinerary itinerary = plan(+1388530860L, "2b1", "2b2", null, true, false, null, "", "", 1);

    validateLeg(itinerary.legs().getFirst(), 1388530980000L, 1388531040000L, "2b2", "2b1", null);

    assertEquals("Stop 2b1 ~ BUS attr 0:03 0:04 ~ Stop 2b2 [C₁90]", itinerary.toStr());
  }
}
