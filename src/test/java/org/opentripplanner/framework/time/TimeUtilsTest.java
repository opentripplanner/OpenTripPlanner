package org.opentripplanner.framework.time;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;

public class TimeUtilsTest {

  private static final ZonedDateTime CAL = ZonedDateTime.of(
    LocalDate.of(2019, Month.JANUARY, 15),
    LocalTime.of(9, 36, 7),
    ZoneIds.UTC
  );

  private final int T00_00_01 = time(0, 0, 1);
  private final int T00_01_00 = time(0, 1, 0);
  private final int T01_00_00 = time(1, 0, 0);
  private final int T09_31_00 = time(9, 31, 0);
  private final int T13_33_57 = time(13, 33, 57);
  private final int T00_58_59 = time(0, 58, 59);
  private final int T26_03_15 = time(26, 3, 15);

  private final int NOT_SET = 999_999;

  @Test
  public void timeToStrCompact() {
    assertEquals("0:00:01", TimeUtils.timeToStrCompact(T00_00_01));
    assertEquals("0:01", TimeUtils.timeToStrCompact(T00_01_00));
    assertEquals("1:00", TimeUtils.timeToStrCompact(T01_00_00));
    assertEquals("23:59:59-1d", TimeUtils.timeToStrCompact(-T00_00_01));
    assertEquals("23:59-1d", TimeUtils.timeToStrCompact(-T00_01_00));
    assertEquals("23:00-1d", TimeUtils.timeToStrCompact(-T01_00_00));

    assertEquals("9:31", TimeUtils.timeToStrCompact(T09_31_00));
    assertEquals("13:33:57", TimeUtils.timeToStrCompact(T13_33_57));
    assertEquals("0:58:59", TimeUtils.timeToStrCompact(T00_58_59));
    assertEquals("13:33:57", TimeUtils.timeToStrCompact(T13_33_57, NOT_SET));
    assertEquals("", TimeUtils.timeToStrCompact(NOT_SET, NOT_SET));
    assertEquals("NOT-SET", TimeUtils.timeToStrCompact(NOT_SET, NOT_SET, "NOT-SET"));
    assertEquals("9:36:07", TimeUtils.timeToStrCompact(CAL));
    assertEquals("10:26:03-1d", TimeUtils.timeToStrCompact(-T13_33_57));
  }

  @Test
  public void timeToStrLong() {
    assertEquals("00:00:01", TimeUtils.timeToStrLong(T00_00_01));
    assertEquals("00:01:00", TimeUtils.timeToStrLong(T00_01_00));
    assertEquals("01:00:00", TimeUtils.timeToStrLong(T01_00_00));
    assertEquals("23:59:59-1d", TimeUtils.timeToStrLong(-T00_00_01));
    assertEquals("23:59:00-1d", TimeUtils.timeToStrLong(-T00_01_00));
    assertEquals("23:00:00-1d", TimeUtils.timeToStrLong(-T01_00_00));

    assertEquals("13:33:57", TimeUtils.timeToStrLong(T13_33_57, NOT_SET));
    assertEquals("", TimeUtils.timeToStrLong(NOT_SET, NOT_SET));
    assertEquals("09:36:07", TimeUtils.timeToStrLong(CAL));
    assertEquals("10:26:03-1d", TimeUtils.timeToStrLong(-T13_33_57));
  }

  @Test
  public void time() {
    // Midnight
    assertEquals(0, TimeUtils.time("0"));
    assertEquals(0, TimeUtils.time("0:0"));
    assertEquals(0, TimeUtils.time("0:0:0"));
    assertEquals(0, TimeUtils.time("0:0:0-0d"));
    assertEquals(0, TimeUtils.time("0:0:0+0d"));
    assertEquals(0, TimeUtils.time("-0:0:0-0d"));

    // Test ONE digit for all 3 formats H, H:M, and H:M:S
    assertEquals(time(3, 0, 0), TimeUtils.time("3"));
    assertEquals(time(3, 2, 0), TimeUtils.time("3:2"));
    assertEquals(time(3, 2, 5), TimeUtils.time("3:2:5"));
    assertEquals(time(-1, 3, 2, 5), TimeUtils.time("3:2:5-1d"));
    assertEquals(time(1, 3, 2, 5), TimeUtils.time("3:2:5+1d"));

    // Test TWO digit for all 3 formats HH, HH:MM, and HH:MM:SS
    assertEquals(time(9, 0, 0), TimeUtils.time("09"));
    assertEquals(time(9, 8, 0), TimeUtils.time("09:08"));
    assertEquals(time(9, 8, 9), TimeUtils.time("09:08:09"));

    // Service time can roll-over into the next day
    assertEquals(time(26, 0, 0), TimeUtils.time("26:00"));
    assertEquals(time(26, 0, 0), TimeUtils.time("02:00+1d"));
    assertEquals(time(-2, 2, 0, 0), TimeUtils.time("02:00-2d"));

    // We also support negative times
    assertEquals(-1, TimeUtils.time("-00:00:01"));
    assertEquals(-60, TimeUtils.time("-00:01:00"));
    assertEquals(-3600, TimeUtils.time("-01:00:00"));
    assertEquals(-time(23, 59, 3), TimeUtils.time("-23:59:03"));
    assertEquals(-time(-2, 1, 2, 3), TimeUtils.time("-1:02:03-2d"));

    // With default value
    assertEquals(time(12, 34, 0), TimeUtils.time("12:34", 7));
    assertEquals(7, TimeUtils.time(null, 7));
    assertEquals(7, TimeUtils.time("", 7));
    assertEquals(7, TimeUtils.time("\t \n", 7));
    assertEquals(time(12, 34, 15), TimeUtils.time(" \n 12:34:15 \t", 7));
  }

  @Test
  public void times() {
    assertEquals("[3600]", Arrays.toString(TimeUtils.times("1")));
    assertEquals("[120]", Arrays.toString(TimeUtils.times("0:2")));
    assertEquals("[3]", Arrays.toString(TimeUtils.times("0:0:3")));
    assertEquals("[3723]", Arrays.toString(TimeUtils.times("01:02:03")));
    assertEquals("[3600, 60, 1]", Arrays.toString(TimeUtils.times("1 0:1 0:0:1")));
    assertEquals("[3600, 60, 1, 7200]", Arrays.toString(TimeUtils.times("1,0:1;0:0:1,; 2")));
  }

  @Test
  public void toZonedDateTime() {
    LocalDate date = LocalDate.of(2020, Month.JANUARY, 15);

    assertEquals("2020-01-15T00:00Z", TimeUtils.zonedDateTime(date, 0, UTC).toString());
    // One second
    assertEquals("2020-01-15T00:00:01Z", TimeUtils.zonedDateTime(date, 1, UTC).toString());
    assertEquals("2020-01-14T23:59:59Z", TimeUtils.zonedDateTime(date, -1, UTC).toString());
    // One minute
    assertEquals("2020-01-15T00:01Z", TimeUtils.zonedDateTime(date, 60, UTC).toString());
    assertEquals("2020-01-14T23:59Z", TimeUtils.zonedDateTime(date, -60, UTC).toString());
    // One hour
    assertEquals("2020-01-15T01:00Z", TimeUtils.zonedDateTime(date, 3600, UTC).toString());
    assertEquals("2020-01-14T23:00Z", TimeUtils.zonedDateTime(date, -3600, UTC).toString());

    // 26h3m15s
    assertEquals("2020-01-16T02:03:15Z", TimeUtils.zonedDateTime(date, T26_03_15, UTC).toString());
    assertEquals("2020-01-13T21:56:45Z", TimeUtils.zonedDateTime(date, -T26_03_15, UTC).toString());
  }

  @Test
  public void toZonedDateTimeDST() {
    ZoneId CET = ZoneIds.OSLO;
    // test daylight-saving-time
    LocalDate D2021_03_28 = LocalDate.of(2021, 3, 28);
    LocalDate D2021_10_31 = LocalDate.of(2021, 10, 31);

    // Given time 00:03:00
    int T00_03 = time(0, 3, 0);

    assertEquals(
      "2021-03-27T23:03+01:00[Europe/Oslo]",
      TimeUtils.zonedDateTime(D2021_03_28, T00_03, CET).toString()
    );
    assertEquals(
      "2021-10-31T01:03+02:00[Europe/Oslo]",
      TimeUtils.zonedDateTime(D2021_10_31, T00_03, CET).toString()
    );

    int T26_03 = time(26, 3, 0);
    assertEquals(
      "2021-03-29T01:03+02:00[Europe/Oslo]",
      TimeUtils.zonedDateTime(D2021_03_28, T26_03, CET).toString()
    );
    assertEquals(
      "2021-11-01T03:03+01:00[Europe/Oslo]",
      TimeUtils.zonedDateTime(D2021_10_31, T26_03, CET).toString()
    );
  }

  @Test
  void testMsToString() {
    assertEquals("0s", TimeUtils.msToString(0));
    assertEquals("0.001s", TimeUtils.msToString(1));
    assertEquals("0.012s", TimeUtils.msToString(12));
    assertEquals("1s", TimeUtils.msToString(1000));
    assertEquals("1.1s", TimeUtils.msToString(1100));
    assertEquals("1.02s", TimeUtils.msToString(1020));
    assertEquals("1.003s", TimeUtils.msToString(1003));
    assertEquals("1.234s", TimeUtils.msToString(1234));

    // Negative numbers
    assertEquals("-1s", TimeUtils.msToString(-1000));
    assertEquals("-1.234s", TimeUtils.msToString(-1234));
  }

  private static int time(int hour, int min, int sec) {
    return 60 * (60 * hour + min) + sec;
  }

  private static int time(int days, int hour, int min, int sec) {
    return 60 * (60 * (24 * days + hour) + min) + sec;
  }
}
