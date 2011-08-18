 
package org.opentripplanner.util;

import java.util.GregorianCalendar;

public class TestUtils {
    public static long dateInSeconds(int year, int month, int day, int hour, int minute, int second) {
        return new GregorianCalendar(year, month, day, hour, minute, second).getTimeInMillis() / 1000;
    }

    public static long toSeconds(GregorianCalendar time) {
        return time.getTimeInMillis() / 1000;
    }
}
