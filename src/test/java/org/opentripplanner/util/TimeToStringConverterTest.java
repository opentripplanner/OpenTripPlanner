package org.opentripplanner.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.util.TimeToStringConverter.parseHH_MM_SS;
import static org.opentripplanner.util.TimeToStringConverter.toHH_MM_SS;

public class TimeToStringConverterTest {

    private static final int SECONDS_IN_MINUTE = 60;

    private static final int SECONDS_IN_HOUR = 3600;

    private static final int T_00_00_00 = 0;

    private static final int T_00_00_01 = 1;

    private static final int T_00_01_00 = SECONDS_IN_MINUTE;

    private static final int T_00_02_01 = 2 * SECONDS_IN_MINUTE + 1;

    private static final int T_01_00_00 = SECONDS_IN_HOUR;

    private static final int T_02_00_01 = 2 * SECONDS_IN_HOUR + 1;

    private static final int T_26_07_01 = 26 * SECONDS_IN_HOUR + 7 * SECONDS_IN_MINUTE + 1;

    private static final int N_00_00_01 = -1;

    private static final int N_00_01_00 = -SECONDS_IN_MINUTE;

    private static final int N_00_02_01 = -(2 * SECONDS_IN_MINUTE + 1);

    private static final int N_01_00_00 = -SECONDS_IN_HOUR;

    private static final int N_02_00_01 = -(2 * SECONDS_IN_HOUR + 1);

    @Test
    public void testToHH_MM_SS() throws Exception {
        assertEquals("00:00:00", toHH_MM_SS(T_00_00_00));
        assertEquals("00:00:01", toHH_MM_SS(T_00_00_01));
        assertEquals("00:01:00", toHH_MM_SS(T_00_01_00));
        assertEquals("00:02:01", toHH_MM_SS(T_00_02_01));
        assertEquals("01:00:00", toHH_MM_SS(T_01_00_00));
        assertEquals("02:00:01", toHH_MM_SS(T_02_00_01));
        assertEquals("26:07:01", toHH_MM_SS(T_26_07_01));
        assertEquals("-00:00:01", toHH_MM_SS(N_00_00_01));
        assertEquals("-00:01:00", toHH_MM_SS(N_00_01_00));
        assertEquals("-00:02:01", toHH_MM_SS(N_00_02_01));
        assertEquals("-01:00:00", toHH_MM_SS(N_01_00_00));
        assertEquals("-02:00:01", toHH_MM_SS(N_02_00_01));
    }

    @Test
    public void testParseHH_MM_SS() {
        assertEquals(T_00_00_00, parseHH_MM_SS("00:00:00"));
        assertEquals(T_00_00_01, parseHH_MM_SS("00:00:01"));
        assertEquals(T_00_01_00, parseHH_MM_SS("00:01:00"));
        assertEquals(T_00_02_01, parseHH_MM_SS("00:02:01"));
        assertEquals(T_01_00_00, parseHH_MM_SS("01:00:00"));
        assertEquals(T_02_00_01, parseHH_MM_SS("02:00:01"));
        assertEquals(T_26_07_01, parseHH_MM_SS("26:07:01"));
        assertEquals(N_00_00_01, parseHH_MM_SS("-00:00:01"));
        assertEquals(N_00_01_00, parseHH_MM_SS("-00:01:00"));
        assertEquals(N_00_02_01, parseHH_MM_SS("-00:02:01"));
        assertEquals(N_01_00_00, parseHH_MM_SS("-01:00:00"));
        assertEquals(N_02_00_01, parseHH_MM_SS("-02:00:01"));
    }

}