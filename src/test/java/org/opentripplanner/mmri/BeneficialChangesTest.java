package org.opentripplanner.mmri;

import org.junit.Ignore;
import org.opentripplanner.model.plan.Leg;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class BeneficialChangesTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/3c";
    }

    public void test3c1() {
        Leg leg = plan(+1388531040L, "3c2", "3c3", null, false, false, null, "", "");

        validateLeg(leg, 1388531040000L, 1388531100000L, "3c3", "3c2", null);
    }
}
