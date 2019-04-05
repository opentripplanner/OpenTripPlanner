package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class TransferTimeTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2a2";
    }

    public void test2a3() {
        Leg[] legs = plan(+1388530860L, "2a3", "2a6", null, false, false, null, "", "", 3);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "2a4", "2a3", null);
        validateLeg(legs[2], 1388531280000L, 1388531340000L, "2a6", "2a5", null);
    }

    public void test2a4() {
        Leg[] legs = plan(+1388530920L, "2a3", "2a6", null, false, false, null, "", "", 3);

        validateLeg(legs[0], 1388531040000L, 1388531100000L, "2a4", "2a3", null);
        validateLeg(legs[2], 1388531400000L, 1388531460000L, "2a6", "2a5", null);
    }

    public void test2a5() {
        Leg[] legs = plan(-1388531460L, "2a3", "2a6", null, false, false, null, "", "", 3);

        validateLeg(legs[0], 1388531040000L, 1388531100000L, "2a4", "2a3", null);
        validateLeg(legs[2], 1388531400000L, 1388531460000L, "2a6", "2a5", null);
    }
}
