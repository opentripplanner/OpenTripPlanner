package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

@Disabled("Requires stop-to-stop transfers without street network")
public class AllModesAndAgenciesTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/1a";
  }

  @Test
  public void test1a1() {
    Itinerary itinerary = plan(+1388530800L, "1a1", "1a6", null, false, false, null, "", "", 5);

    Leg[] legs = itinerary.legs.toArray(new Leg[5]);

    validateLeg(legs[0], 1388530860000L, 1388530920000L, "1a2", "1a1", null);
    validateLeg(legs[2], 1388530980000L, 1388531040000L, "1a4", "1a3", null);
    validateLeg(legs[4], 1388531100000L, 1388531160000L, "1a6", "1a5", null);

    assertEquals("", itinerary.toStr());
  }
}
