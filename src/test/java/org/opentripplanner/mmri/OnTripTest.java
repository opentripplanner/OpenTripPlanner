package org.opentripplanner.mmri;

import org.junit.Ignore;
import org.opentripplanner.api.model.Leg;

/**
 * //TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class OnTripTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2f";
    }

    public void test2f1() {
        Leg[] legs = plan(+1388530920L, null, "2f2", "2f|intercity", false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530920000L, 1388531040000L, "2f3", null, null);
        validateLeg(legs[1], 1388531160000L, 1388531340000L, "2f2", "2f3", null);
    }
}
