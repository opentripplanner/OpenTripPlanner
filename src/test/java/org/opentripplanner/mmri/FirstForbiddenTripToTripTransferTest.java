package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class FirstForbiddenTripToTripTransferTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2e3";
    }

    public void test2e3() {
        Leg[] legs = plan(+1388530860L, "2e31", "2e36", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530980000L, "2e34", "2e31", null);
        validateLeg(legs[1], 1388531040000L, 1388531100000L, "2e36", "2e34", null);
    }
}
