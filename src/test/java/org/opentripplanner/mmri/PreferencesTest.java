package org.opentripplanner.mmri;

import static org.opentripplanner.routing.core.TraverseMode.BUS;

import org.opentripplanner.api.model.Leg;

public class PreferencesTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2c";
    }

    public void test2c1() {
        Leg[] legs = plan(+1388530860L, "2c1", "2c3", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "2c2", "2c1", null);
        validateLeg(legs[1], 1388530980000L, 1388531040000L, "2c3", "2c2", null);
    }

    public void test2c2() {
        Leg[] legs = plan(+1388530860L, "2c1", "2c3", null, false, false, BUS, "", "", 3);

        validateLeg(legs[1], 1388530920000L, 1388531160000L, "2c5", "2c4", null);
    }

    public void test2c3() {
        Leg[] legs = plan(+1388530860L, "2c1", "2c3", null, false, true, null, "", "", 3);

        validateLeg(legs[1], 1388530920000L, 1388531160000L, "2c5", "2c4", null);
    }
}
