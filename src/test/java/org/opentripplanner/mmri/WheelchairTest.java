package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class WheelchairTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2b";
    }

    public void test2b1() {
        Leg leg = plan(+1388530860L, "2b1", "2b2", null, true, false, null, "", "");

        validateLeg(leg, 1388530980000L, 1388531040000L, "2b2", "2b1", null);
    }
}
