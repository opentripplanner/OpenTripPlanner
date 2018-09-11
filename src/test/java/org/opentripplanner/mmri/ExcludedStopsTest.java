package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class ExcludedStopsTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/3f";
    }

    public void test3f1() {
        Leg leg = plan(+1388530860L, "3f1", "3f3", null, false, false, null, "", "3f2");

        validateLeg(leg, 1388530860000L, 1388531040000L, "3f3", "3f1", null);
    }
}
