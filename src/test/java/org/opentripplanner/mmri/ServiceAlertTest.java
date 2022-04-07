package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

@Disabled("Service alerts not mapped correctly")
public class ServiceAlertTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/3i";
  }

  @Test
  public void test3i1() {
    Itinerary itinerary = plan(+1388530860L, "3i1", "3i2", null, false, false, null, "", "", 1);

    Leg leg = itinerary.legs.toArray(new Leg[1])[0];

    validateLeg(leg, 1388530860000L, 1388530920000L, "3i2", "3i1", "Unknown effect");

    assertEquals("", itinerary.toStr());
  }
}
