package org.opentripplanner.framework.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.time.ServiceDateUtils.asStartOfService;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;

public class ServiceDateUtilsTest {

  private static final ZoneId ZONE_ID = ZoneIds.PARIS;
  private static final LocalTime TIME = LocalTime.of(10, 26);

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

  // Zoned dates in spring around DST switch
  private static final ZonedDateTime Z1 = ZonedDateTime.of(D2019_03_30, TIME, ZONE_ID);
  private static final ZonedDateTime Z2 = ZonedDateTime.of(D2019_03_31, TIME, ZONE_ID);
  private static final ZonedDateTime Z3 = ZonedDateTime.of(D2019_04_01, TIME, ZONE_ID);

  // Zoned dates in fall around DST switch
  private static final ZonedDateTime Z4 = ZonedDateTime.of(D2019_10_26, TIME, ZONE_ID);
  private static final ZonedDateTime Z5 = ZonedDateTime.of(D2019_10_27, TIME, ZONE_ID);
  private static final ZonedDateTime Z6 = ZonedDateTime.of(D2019_10_28, TIME, ZONE_ID);

  @Test
  public void testAsStartOfServiceWithZonedDatesAroundDST() {
    // Test Zoned dates around DST
    assertEquals("2019-03-30T00:00+01:00[Europe/Paris]", asStartOfService(Z1).toString());
    assertEquals("2019-03-30T23:00+01:00[Europe/Paris]", asStartOfService(Z2).toString());
    assertEquals("2019-04-01T00:00+02:00[Europe/Paris]", asStartOfService(Z3).toString());
    assertEquals("2019-10-26T00:00+02:00[Europe/Paris]", asStartOfService(Z4).toString());
    assertEquals("2019-10-27T01:00+02:00[Europe/Paris]", asStartOfService(Z5).toString());
    assertEquals("2019-10-28T00:00+01:00[Europe/Paris]", asStartOfService(Z6).toString());
  }

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
