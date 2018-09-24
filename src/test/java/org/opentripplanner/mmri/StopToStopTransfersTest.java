package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class StopToStopTransfersTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2d";
    }

    public void test2d1() {
        Leg[] legs = plan(+1388530860L, "2d1", "2d4", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530980000L, "2d3", "2d1", null);
        validateLeg(legs[1], 1388530980000L, 1388531040000L, "2d4", "2d3", null);
    }
}
