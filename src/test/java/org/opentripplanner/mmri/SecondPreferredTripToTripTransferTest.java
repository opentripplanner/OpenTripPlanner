package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class SecondPreferredTripToTripTransferTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2e2";
    }

    public void test2e2() {
        Leg[] legs = plan(+1388530860L, "2e21", "2e26", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530980000L, "2e24", "2e21", null);
        validateLeg(legs[1], 1388531040000L, 1388531100000L, "2e26", "2e24", null);
    }
}
