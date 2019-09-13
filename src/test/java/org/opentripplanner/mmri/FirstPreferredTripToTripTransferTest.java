package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class FirstPreferredTripToTripTransferTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2e1";
    }

    public void test2e1() {
        Leg[] legs = plan(+1388530860L, "2e11", "2e16", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "2e13", "2e11", null);
        validateLeg(legs[1], 1388530980000L, 1388531100000L, "2e16", "2e13", null);
    }
}
