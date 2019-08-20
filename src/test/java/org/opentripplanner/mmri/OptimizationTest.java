package org.opentripplanner.mmri;

import org.junit.Ignore;
import org.opentripplanner.api.model.Leg;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class OptimizationTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2a1";
    }

    public void test2a1() {
        Leg leg = plan(+1388530860L, "2a1", "2a2", null, false, false, null, "", "");

        validateLeg(leg, 1388530860000L, 1388530920000L, "2a2", "2a1", null);
    }

    public void test2a2() {
        Leg leg = plan(+1388530980L, "2a1", "2a2", null, false, false, null, "", "");

        validateLeg(leg, 1388531100000L, 1388531160000L, "2a2", "2a1", null);
    }
}
