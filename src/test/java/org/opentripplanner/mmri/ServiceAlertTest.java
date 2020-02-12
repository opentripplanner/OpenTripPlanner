package org.opentripplanner.mmri;

import org.junit.Ignore;
import org.opentripplanner.model.plan.Leg;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class ServiceAlertTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/3i";
    }

    public void test3i1() {
        Leg leg = plan(+1388530860L, "3i1", "3i2", null, false, false, null, "", "");

        validateLeg(leg, 1388530860000L, 1388530920000L, "3i2", "3i1", "Unknown effect");
    }
}
