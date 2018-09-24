package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class PlannerstackScenarioTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/plannerstack_scenario";
    }

    public void testPlannerstackScenario() {
        Leg[] legs = plan(+1388531220L, null, "plannerstack_scenario2",
                "plannerstack_scenario|intercity", false, false, null, "", "", 2);

        validateLeg(legs[0], 1388531220000L, 1388531340000L, "plannerstack_scenario3", null, null);
        validateLeg(legs[1], 1388531400000L, 1388531640000L, "plannerstack_scenario2",
                "plannerstack_scenario3", null);
    }
}
