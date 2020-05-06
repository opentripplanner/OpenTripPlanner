package org.opentripplanner.transit.raptor.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class TimeUtilsTest {

    private final static Calendar CAL = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final int T_09_31_00 = time(9, 31, 0);
    private final int T_09_36_55 = time(9, 36, 55);
    private final int T_13_33_57 = time(13, 33, 57);
    private final int T_14_00_47 = time(14, 0, 47);
    private final int T_15_37_00 = time(15, 37, 0);
    private final int T_00_58_59 = time(0, 58, 59);
    private final int T_00_05_51 = time(0, 5, 51);
    private final int T_00_00_09 = time(0, 0, 9);
    private final int NOT_SET = 999_999;

    static {
        CAL.clear();
        CAL.set(2019, Calendar.JANUARY, 15, 9, 36, 7);
    }

    @Test
    public void timeToStrCompact() {
        assertEquals("9:31", TimeUtils.timeToStrCompact(T_09_31_00));
        assertEquals("9:36:55", TimeUtils.timeToStrCompact(T_09_36_55));
        assertEquals("13:33:57", TimeUtils.timeToStrCompact(T_13_33_57));
        assertEquals("0:58:59", TimeUtils.timeToStrCompact(T_00_58_59));
        assertEquals("0:05:51", TimeUtils.timeToStrCompact(T_00_05_51));
        assertEquals("0:00:09", TimeUtils.timeToStrCompact(T_00_00_09));
        assertEquals("13:33:57", TimeUtils.timeToStrCompact(T_13_33_57, NOT_SET));
        assertEquals("", TimeUtils.timeToStrCompact(NOT_SET, NOT_SET));
        assertEquals("9:36:07", TimeUtils.timeToStrCompact(CAL));
        assertEquals("-13:33:57", TimeUtils.timeToStrCompact(-T_13_33_57));
        assertEquals("-0:00:09", TimeUtils.timeToStrCompact(-T_00_00_09));
    }

    @Test
    public void timeToStrLong() {
        assertEquals("09:31:00", TimeUtils.timeToStrLong(T_09_31_00));
        assertEquals("09:36:55", TimeUtils.timeToStrLong(T_09_36_55));
        assertEquals("13:33:57", TimeUtils.timeToStrLong(T_13_33_57));
        assertEquals("00:00:09", TimeUtils.timeToStrLong(T_00_00_09));
        assertEquals("13:33:57", TimeUtils.timeToStrLong(T_13_33_57, NOT_SET));
        assertEquals("", TimeUtils.timeToStrLong(NOT_SET, NOT_SET));
        assertEquals("09:36:07", TimeUtils.timeToStrLong(CAL));
        assertEquals("-13:33:57", TimeUtils.timeToStrLong(-T_13_33_57));
        assertEquals("-00:00:09", TimeUtils.timeToStrLong(-T_00_00_09));
    }

    @Test
    public void timeToStrShort() {
        assertEquals("09:31", TimeUtils.timeToStrShort(T_09_31_00));
        assertEquals("09:36", TimeUtils.timeToStrShort(T_09_36_55));
        assertEquals("13:33", TimeUtils.timeToStrShort(T_13_33_57));
        assertEquals("00:00", TimeUtils.timeToStrShort(T_00_00_09));
        assertEquals("09:36", TimeUtils.timeToStrShort(CAL));
        assertEquals("-13:33", TimeUtils.timeToStrShort(-T_13_33_57));
        assertEquals("-00:00", TimeUtils.timeToStrShort(-T_00_00_09));
    }

    @Test
    public void durationToStr() {
        assertEquals("9h36m55s", TimeUtils.durationToStr(T_09_36_55));
        assertEquals("13h33m57s", TimeUtils.durationToStr(T_13_33_57));
        assertEquals("9h31m", TimeUtils.durationToStr(T_09_31_00));
        assertEquals("9s", TimeUtils.durationToStr(T_00_00_09));
        assertEquals("14h47s", TimeUtils.durationToStr(T_14_00_47));
        assertEquals("15h37m", TimeUtils.durationToStr(T_15_37_00));
        assertEquals("-13h33m57s", TimeUtils.durationToStr(-T_13_33_57));
        assertEquals("-9h31m", TimeUtils.durationToStr(-T_09_31_00));
        assertEquals("-9s", TimeUtils.durationToStr(-T_00_00_09));
        assertEquals("", TimeUtils.durationToStr(NOT_SET, NOT_SET));
    }

    @Test
    public void timeMsToStrInSec() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            assertEquals("0 seconds", TimeUtils.msToSecondsStr(0));
            assertEquals("0,001 seconds", TimeUtils.msToSecondsStr(1));
            assertEquals("0,099 seconds", TimeUtils.msToSecondsStr(99));
            assertEquals("0,10 seconds", TimeUtils.msToSecondsStr(100));
            assertEquals("0,99 seconds", TimeUtils.msToSecondsStr(994));
            assertEquals("1,0 seconds", TimeUtils.msToSecondsStr(995));
            assertEquals("1,0 seconds", TimeUtils.msToSecondsStr(999));
            assertEquals("1 second", TimeUtils.msToSecondsStr(1000));
            assertEquals("1,0 seconds", TimeUtils.msToSecondsStr(1001));
            assertEquals("9,9 seconds", TimeUtils.msToSecondsStr(9_949));
            assertEquals("10 seconds", TimeUtils.msToSecondsStr(9_950));
            assertEquals("-0,456 seconds", TimeUtils.msToSecondsStr(-456));
        }
        finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void midnightOf() {
        // When
        Calendar midnight = TimeUtils.midnightOf(CAL);

        // Then
        assertEquals(0, midnight.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, midnight.get(Calendar.MINUTE));
        assertEquals(0, midnight.get(Calendar.SECOND));
        assertEquals(0, midnight.get(Calendar.MILLISECOND));
    }

    private static int time(int hour, int min, int sec) {
        return 60 * (60 * hour + min) + sec;
    }
}