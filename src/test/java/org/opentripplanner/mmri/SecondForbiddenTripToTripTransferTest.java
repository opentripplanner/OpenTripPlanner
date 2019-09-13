package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class SecondForbiddenTripToTripTransferTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2e4";
    }

    public void test2e4() {
        Leg[] legs = plan(+1388530860L, "2e41", "2e46", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "2e43", "2e41", null);
        validateLeg(legs[1], 1388530980000L, 1388531100000L, "2e46", "2e43", null);
    }
}
