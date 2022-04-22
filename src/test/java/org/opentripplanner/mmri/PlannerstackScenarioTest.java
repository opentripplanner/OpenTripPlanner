package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

@Disabled("Requires departing onboard a trip")
public class PlannerstackScenarioTest extends GtfsTest {

  @Override
  public final String getFeedName() {
    return "mmri/plannerstack_scenario";
  }

  @Test
  public void testPlannerstackScenario() {
    Itinerary itinerary = plan(
      +1388531220L,
      null,
      "plannerstack_scenario2",
      "plannerstack_scenario|intercity",
      false,
      false,
      null,
      "",
      "",
      2
    );

    Leg[] legs = itinerary.legs.toArray(new Leg[2]);

    validateLeg(legs[0], 1388531220000L, 1388531340000L, "plannerstack_scenario3", null, null);
    validateLeg(
      legs[1],
      1388531400000L,
      1388531640000L,
      "plannerstack_scenario2",
      "plannerstack_scenario3",
      null
    );

    assertEquals("", itinerary.toStr());
  }
}
