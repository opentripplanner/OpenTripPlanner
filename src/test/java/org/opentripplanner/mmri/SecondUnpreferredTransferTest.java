package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class SecondUnpreferredTransferTest extends MmriTest {

    @Override
    public final String getFeedName() {
        return "mmri/3g2";
    }

    public void test3g2() {
        Leg[] legs = plan(+1388530860L, "3g21", "3g26", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "3g23", "3g21", null);
        validateLeg(legs[1], 1388530980000L, 1388531100000L, "3g26", "3g23", null);
    }
}
