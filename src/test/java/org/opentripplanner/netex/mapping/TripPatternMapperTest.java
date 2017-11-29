package org.opentripplanner.netex.mapping;

import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalTime;

import static org.junit.Assert.*;
import static org.opentripplanner.netex.mapping.TripPatternMapper.*;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
public class TripPatternMapperTest {

    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private static final LocalTime QUARTER_PAST_FIVE = LocalTime.of(5, 15);

    @Test
    public void testCalculateOtpTime() throws Exception {
        assertEquals(18900, calculateOtpTime(QUARTER_PAST_FIVE, ZERO));
        assertEquals(105300, calculateOtpTime(QUARTER_PAST_FIVE, ONE));
        assertEquals(191700, calculateOtpTime(QUARTER_PAST_FIVE, TWO));
    }

}