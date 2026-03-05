package org.opentripplanner.utils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.utils.time.ServiceDateUtils.asStartOfService;
import static org.opentripplanner.utils.time.ServiceDateUtils.calculateRunningDates;
import static org.opentripplanner.utils.time.ServiceDateUtils.wholeDays;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ServiceDateUtilsTest {

  private static final ZoneId ZONE_ID = ZoneIds.PARIS;

  private static final LocalDate D2019_03_30 = LocalDate.of(2019, 3, 30);
  // Daylight Saving Time change from Winter to Summer time on MAR 03 2019 in Europe
  private static final LocalDate D2019_03_31 = LocalDate.of(2019, 3, 31);
  private static final LocalDate D2019_04_01 = LocalDate.of(2019, 4, 1);

  private static final LocalDate D2019_10_26 = LocalDate.of(2019, 10, 26);
  // Daylight Saving Time change from Summer to Winter time on OCT 27 2019 in Europe
  private static final LocalDate D2019_10_27 = LocalDate.of(2019, 10, 27);
  private static final LocalDate D2019_10_28 = LocalDate.of(2019, 10, 28);

  private static final ZonedDateTime Z0 = ZonedDateTime.of(
    D2019_03_30,
    LocalTime.MIDNIGHT,
    ZONE_ID
  );
  public static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Europe/Oslo");

  @Test
  public void testAsStartOfServiceWithLocalDatesAndZoneAroundDST() {
    // Test LocalDates with Zone around DST
    assertEquals(
      "2019-03-30T00:00+01:00[Europe/Paris]",
      asStartOfService(D2019_03_30, ZONE_ID).toString()
    );
    assertEquals(
      "2019-03-30T23:00+01:00[Europe/Paris]",
      asStartOfService(D2019_03_31, ZONE_ID).toString()
    );
    assertEquals(
      "2019-04-01T00:00+02:00[Europe/Paris]",
      asStartOfService(D2019_04_01, ZONE_ID).toString()
    );
    assertEquals(
      "2019-10-26T00:00+02:00[Europe/Paris]",
      asStartOfService(D2019_10_26, ZONE_ID).toString()
    );
    assertEquals(
      "2019-10-27T01:00+02:00[Europe/Paris]",
      asStartOfService(D2019_10_27, ZONE_ID).toString()
    );
    assertEquals(
      "2019-10-28T00:00+01:00[Europe/Paris]",
      asStartOfService(D2019_10_28, ZONE_ID).toString()
    );
  }

  @Test
  public void testAsStartOfServiceWithInstance() {
    var time = Instant.parse("2019-03-30T10:00:00Z");
    assertEquals(
      "2019-03-30T00:00+01:00[Europe/Paris]",
      asStartOfService(time, ZONE_ID).toString()
    );
  }

  @Test
  public void test() {
    var sd1 = asStartOfService(D2019_03_30, ZONE_ID);
    var sd2 = asStartOfService(D2019_03_31, ZONE_ID);
    var sd3 = asStartOfService(D2019_04_01, ZONE_ID);

    assertEquals(D2019_03_30, ServiceDateUtils.asServiceDay(sd1));
    assertEquals(D2019_03_31, ServiceDateUtils.asServiceDay(sd2));
    assertEquals(D2019_04_01, ServiceDateUtils.asServiceDay(sd3));

    // When DST adjustment happens the startTime is not on the same date as
    // the serviceDate
    assertEquals(sd1.toLocalDate(), ServiceDateUtils.asServiceDay(sd1));
    assertNotEquals(sd2.toLocalDate(), ServiceDateUtils.asServiceDay(sd2));
    assertEquals(sd3.toLocalDate(), ServiceDateUtils.asServiceDay(sd3));
  }

  @Test
  public void getStartOfService() {
    var zone = ZoneIds.OSLO;
    LocalDate d = LocalDate.of(2020, 8, 25);

    assertEquals(
      "2020-08-25T00:00+02:00[Europe/Oslo]",
      ServiceDateUtils.asStartOfService(d, zone).toString()
    );

    // Time is adjusted 1 hour back in Norway on this date
    d = LocalDate.of(2020, 10, 25);
    assertEquals(
      "2020-10-25T01:00+02:00[Europe/Oslo]",
      ServiceDateUtils.asStartOfService(d, zone).toString()
    );

    // Time is adjusted 1 hour forward in Norway on this date
    d = LocalDate.of(2020, 3, 29);
    assertEquals(
      "2020-03-28T23:00+01:00[Europe/Oslo]",
      ServiceDateUtils.asStartOfService(d, zone).toString()
    );
  }

  @Test
  public void secondsSinceStartOfTime() {
    assertEquals(0, ServiceDateUtils.secondsSinceStartOfTime(Z0, D2019_03_30));
    assertEquals(23 * 3600, ServiceDateUtils.secondsSinceStartOfTime(Z0, D2019_03_31));
    assertEquals((23 + 24) * 3600, ServiceDateUtils.secondsSinceStartOfTime(Z0, D2019_04_01));

    // Test the Instant version of this method too
    Instant instant = D2019_04_01.atStartOfDay(ZONE_ID).toInstant();
    assertEquals((23 + 24) * 3600, ServiceDateUtils.secondsSinceStartOfTime(Z0, instant));
  }

  @Test
  public void secondsSinceStartOfTimeWithZoneId() {
    var dateTime = ZonedDateTime.parse("2022-05-12T00:43:00+02:00");
    var operatingDayDate = dateTime.minusDays(1).withZoneSameInstant(ZoneIds.UTC);
    var zoneId = ZoneIds.CET;
    var startOfService = ZonedDateTime.of(2022, 5, 11, 0, 0, 0, 0, zoneId);

    var desiredDuration = Duration.between(startOfService, dateTime).toSeconds();
    assertEquals(
      desiredDuration,
      ServiceDateUtils.secondsSinceStartOfService(operatingDayDate, dateTime, zoneId)
    );
  }

  /// The input time is in UTC and the the transit service time zone(Europe/Oslo) which is
  /// +1 hour in winter and +2 in summer. This means that we switch to a new service-day at
  /// 23:00Z in winter time and at 22:00Z in summer time.
  ///
  /// Note! The last test cases test the transition from summer-time to winter-time and back.
  /// Day light savings is adjusted:
  /// - 29. March 2026    02:00 -> 03:00  First summer service-day start at 23:00, 1h overlap
  /// - 25. October 2026  03:00 -> 02:00  First winter service-day start at 01:00, 1h gap
  ///
  @ParameterizedTest
  @CsvSource(
    value = """
    Start time           | Window | days | Expected
    2026-02-10T12:00:00Z | 60m    | 0    | 2026-02-10
    2026-02-10T12:00:00Z | 60m    | 1    | 2026-02-09, 2026-02-10
    2026-02-10T12:00:00Z | 60m    | 3    | 2026-02-07, 2026-02-08, 2026-02-09, 2026-02-10
    2026-02-10T22:29:59Z | 30m    | 0    | 2026-02-10
    2026-02-10T22:30:00Z | 30m    | 0    | 2026-02-10, 2026-02-11
    2026-02-10T22:59:59Z | 30m    | 0    | 2026-02-10, 2026-02-11
    2026-02-10T23:00:00Z | 30m    | 0    | 2026-02-11
    2026-08-10T21:39:59Z | 20m    | 0    | 2026-08-10
    2026-08-10T21:40:00Z | 20m    | 0    | 2026-08-10, 2026-08-11
    2026-08-10T21:59:59Z | 20m    | 0    | 2026-08-10, 2026-08-11
    2026-08-10T22:00:00Z | 20m    | 0    | 2026-08-11
    2026-03-28T21:49:59Z | 10m    | 0    | 2026-03-28
    2026-03-28T21:50:00Z | 10m    | 0    | 2026-03-28, 2026-03-29
    2026-10-23T21:49:59Z | 10m    | 0    | 2026-10-23
    2026-10-23T21:50:00Z | 10m    | 0    | 2026-10-23, 2026-10-24
    """,
    delimiter = '|',
    useHeadersInDisplayName = true
  )
  public void testCalculateRunningDates(
    Instant startTime,
    String window,
    int maxTripSpanDays,
    String expectedInput
  ) {
    var win = DurationUtils.duration(window);

    var result = calculateRunningDates(startTime, win, SERVICE_ZONE_ID, maxTripSpanDays);

    var expected = Arrays.stream(expectedInput.split(", ")).map(LocalDate::parse).toList();
    assertEquals(expected, result);
  }

  @Test
  public void testWholeDays() {
    int secondsInOneDay = 86400;
    int secondsInTenDays = 10 * secondsInOneDay;
    assertEquals(0, wholeDays(-secondsInTenDays));
    assertEquals(0, wholeDays(-1));
    assertEquals(0, wholeDays(0));
    assertEquals(0, wholeDays(secondsInOneDay - 1));
    assertEquals(1, wholeDays(secondsInOneDay));
    assertEquals(9, wholeDays(secondsInTenDays - 1));
    assertEquals(10, wholeDays(secondsInTenDays));
  }

  @Test
  public void parse() throws ParseException {
    LocalDate subject;

    subject = ServiceDateUtils.parseString("20201231");
    assertEquals(2020, subject.getYear());
    assertEquals(Month.DECEMBER, subject.getMonth());
    assertEquals(31, subject.getDayOfMonth());

    subject = ServiceDateUtils.parseString("2020-03-12");
    assertEquals(2020, subject.getYear());
    assertEquals(Month.MARCH, subject.getMonth());
    assertEquals(12, subject.getDayOfMonth());

    // Even though this is a valid date, we only support parsing of dates with
    // 4 digits in the year
    assertThrows(
      ParseException.class,
      () -> ServiceDateUtils.parseString("0-03-12"),
      "error parsing date: 0-03-12"
    );
  }

  @Test
  public void minMax() throws ParseException {
    LocalDate d1 = LocalDate.parse("2020-12-30");
    LocalDate d2 = LocalDate.parse("2020-12-31");

    assertSame(d1, ServiceDateUtils.min(d1, d2));
    assertSame(d1, ServiceDateUtils.min(d2, d1));
    assertSame(d2, ServiceDateUtils.max(d1, d2));
    assertSame(d2, ServiceDateUtils.max(d2, d1));

    // Test isMinMax
    assertFalse(ServiceDateUtils.isMinMax(d1));
    assertTrue(ServiceDateUtils.isMinMax(LocalDate.MIN));
    assertTrue(ServiceDateUtils.isMinMax(LocalDate.MAX));
  }

  @Test
  public void asCompactString() {
    assertEquals("-9999999990101", ServiceDateUtils.asCompactString(LocalDate.MIN));
    assertEquals("+9999999991231", ServiceDateUtils.asCompactString(LocalDate.MAX));
    assertEquals("20200312", ServiceDateUtils.asCompactString(LocalDate.of(2020, 3, 12)));
  }

  @Test
  public void testToString() {
    assertEquals("MAX", ServiceDateUtils.toString(LocalDate.MAX));
    assertEquals("MIN", ServiceDateUtils.toString(LocalDate.MIN));
    assertEquals("2020-03-12", ServiceDateUtils.toString(LocalDate.of(2020, 3, 12)));
  }
}
