package org.opentripplanner.util.time;

import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.util.time.DateUtils.secToHHMM;

public class DateUtilsTest {

    // Create some time constants: T<hour>_<minutes>(_<seconds>)? 
    private static final int T00_00 = 0;
    private static final int T00_00_01 = 1;
    private static final int T00_00_59 = 59;
    private static final int T00_01 = 60;
    private static final int T00_05 = 300;
    private static final int T08_07 = (8 * 60 + 7) * 60;
    private static final int T08_47 = (8 * 60 + 47) * 60;
    private static final int T35_00 = 35 * 3600;
    
    // Create some negative time constants: N<hour>_<minutes>(_<seconds>)? 
    private static final int N00_00_01 = -1;
    private static final int N00_00_59 = -59;
    private static final int N00_05 = -300;
    private static final int N08_00 = -8 * 3600;
    private static final int N08_07 = -(8 * 60 + 7) * 60;
    private static final int N08_47 = -(8 * 60 + 47) * 60;





    @Test
    public final void testToDate() {
        Date date = DateUtils.toDate("1970-01-01", "00:00", TimeZone.getTimeZone("UTC"));
        assertEquals(0, date.getTime());

        date = DateUtils.toDate(null, "00:00", TimeZone.getTimeZone("UTC"));
        assertEquals(0, date.getTime() % DateUtils.ONE_DAY_MILLI);
    }

    @Test
    public final void testSecToHHMM() {
        assertEquals("Handle zero", "0:00", secToHHMM(T00_00));
        assertEquals("Skip seconds(1 sec)", "0:00", secToHHMM(T00_00_01));
        assertEquals("Skip seconds(59 sec), round down", "0:00", secToHHMM(T00_00_59));
        assertEquals("1 minute with leading zero", "0:01", secToHHMM(T00_01));
        assertEquals("5 minutes", "0:05", secToHHMM(T00_05));
        assertEquals("Hour and min with leading zero on minute", "8:07", secToHHMM(T08_07));
        assertEquals("8 hours and 47 minutes", "8:47", secToHHMM(T08_47));
        assertEquals("allow ServiceTime beyond 24 hours", "35:00", secToHHMM(T35_00));

        // Negative times
        assertEquals("1 sec - round to minus zero", "-0:00", secToHHMM(N00_00_01));
        assertEquals("59 sec - round down with minus sign", "-0:00", secToHHMM(N00_00_59));
        assertEquals("minus 5 min", "-0:05", secToHHMM(N00_05));
        assertEquals("-8:00", secToHHMM(N08_00));
        assertEquals( "-8:07", secToHHMM(N08_07));
        assertEquals( "-8:47", secToHHMM(N08_47));
    }
}
