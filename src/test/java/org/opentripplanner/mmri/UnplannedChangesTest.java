package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class UnplannedChangesTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/3b";
    }

    public void test3b1() {
        Leg leg = plan(+1388530860L, "3b1", "3b2", null, false, false, null, "", "");

        validateLeg(leg, 1388531460000L, 1388531520000L, "3b2", "3b1", null);
    }

    public void test3b2() {
        Leg leg = plan(+1388531460L, "3b1", "3b2", null, false, false, null, "", "");

        validateLeg(leg, 1388531460000L, 1388531520000L, "3b2", "3b1", null);
    }
}
