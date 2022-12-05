package org.opentripplanner.framework.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.DateUtils.secToHHMM;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;

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

  public static final ZoneId UTC = ZoneIds.UTC;

  @Test
  public final void testToDate() {
    ZonedDateTime date = DateUtils.toZonedDateTime("1970-01-01", "00:00", UTC);
    assertEquals("1970-01-01", date.toLocalDate().toString());
    assertEquals(0, date.toEpochSecond());

    date = DateUtils.toZonedDateTime(null, "00:00", UTC);
    assertEquals(LocalDate.now(UTC).toString(), date.toLocalDate().toString());
    assertEquals(0, date.toEpochSecond() % TimeUtils.ONE_DAY_SECONDS);
  }

  @Test
  public final void testSecToHHMM() {
    assertEquals("0:00", secToHHMM(T00_00), "Handle zero");
    assertEquals("0:00", secToHHMM(T00_00_01), "Skip seconds(1 sec)");
    assertEquals("0:00", secToHHMM(T00_00_59), "Skip seconds(59 sec), round down");
    assertEquals("0:01", secToHHMM(T00_01), "1 minute with leading zero");
    assertEquals("0:05", secToHHMM(T00_05), "5 minutes");
    assertEquals("8:07", secToHHMM(T08_07), "Hour and min with leading zero on minute");
    assertEquals("8:47", secToHHMM(T08_47), "8 hours and 47 minutes");
    assertEquals("35:00", secToHHMM(T35_00), "allow ServiceTime beyond 24 hours");

    // Negative times
    assertEquals("-0:00", secToHHMM(N00_00_01), "1 sec - round to minus zero");
    assertEquals("-0:00", secToHHMM(N00_00_59), "59 sec - round down with minus sign");
    assertEquals("-0:05", secToHHMM(N00_05), "minus 5 min");
    assertEquals("-8:00", secToHHMM(N08_00));
    assertEquals("-8:07", secToHHMM(N08_07));
    assertEquals("-8:47", secToHHMM(N08_47));
  }
}
