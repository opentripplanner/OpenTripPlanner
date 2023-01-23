package org.opentripplanner.transit.model.calendar;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;

class CalendarDaysTest {

  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Oslo");
  private static final int T_ZERO = 0;
  private static final int T07_00 = (int) Duration.ofHours(7).toSeconds();
  private static final int T26_30 = (int) Duration.ofHours(26).plusMinutes(30).toSeconds();
  private static final int D23h = (int) Duration.ofHours(23).toSeconds();
  private static final int D24h = (int) Duration.ofHours(24).toSeconds();
  private static final int D25h = (int) Duration.ofHours(25).toSeconds();
  public static final int JAN_1 = 0;
  public static final int MAR_26 = 84;
  public static final int MAR_27 = 85;
  public static final int OCT_29 = 301;
  public static final int OCT_30 = 302;
  public static final int DEC_31 = 364;

  private static final List<Arguments> testCases = List.of(
    // "Time Zero" is 04:00  1. Jan 2022
    tc("2022-01-01T04:00:00+01:00[Europe/Oslo]", JAN_1, T_ZERO),
    // Offset 07:00 (+04:00) = 11:00
    tc("2022-01-01T11:00:00+01:00[Europe/Oslo]", JAN_1, T07_00),
    // Offset 26:30 (+04:00) = 30:30
    tc("2022-01-02T06:30:00+01:00[Europe/Oslo]", JAN_1, T26_30),
    // Day before DST adjustment (winter time)
    tc("2022-03-26T04:00:00+01:00[Europe/Oslo]", MAR_26, T_ZERO),
    // Day of DST adjustment (to summer time)
    tc("2022-03-27T04:00:00+02:00[Europe/Oslo]", MAR_27, T_ZERO),
    // Day before DST adjustment (summer time)
    tc("2022-10-29T04:00:00+02:00[Europe/Oslo]", OCT_29, T_ZERO),
    // Day of DST adjustment (to winter time)
    tc("2022-10-30T04:00:00+01:00[Europe/Oslo]", OCT_30, T_ZERO),
    // Last day in transit period (inclusive)
    tc("2022-12-31T04:00:00+01:00[Europe/Oslo]", DEC_31, T_ZERO)
  );

  private final CalendarDays subject;

  public CalendarDaysTest() {
    subject =
      CalendarDays
        .of()
        .withPeriodStart(LocalDate.of(2022, Month.JANUARY, 1))
        .withPeriodEnd(LocalDate.of(2022, Month.DECEMBER, 31))
        .withOffset(Duration.ofHours(4))
        .withZoneId(TIME_ZONE)
        .build();
  }

  @Test
  void dayLengthSeconds() {
    // 24 expected for 1. Jan 2022
    Assertions.assertEquals(D24h, subject.dayLengthSeconds(JAN_1));

    // Day before DST adjustment (winter time)
    Assertions.assertEquals(D23h, subject.dayLengthSeconds(MAR_26));

    // Day of DST adjustment (to summer time)
    Assertions.assertEquals(D24h, subject.dayLengthSeconds(MAR_27));

    // Day before DST adjustment (summer time)
    Assertions.assertEquals(D25h, subject.dayLengthSeconds(OCT_29));

    // Day of DST adjustment (to winter time)
    Assertions.assertEquals(D24h, subject.dayLengthSeconds(OCT_30));

    // Last day of calendar defined
    Assertions.assertEquals(D24h, subject.dayLengthSeconds(DEC_31));
  }

  @Test
  void assertLessThanMaxSizeForCalendar() {
    CalendarDays
      .of()
      .withPeriodStart(LocalDate.of(2000, Month.JANUARY, 1))
      .withPeriodEnd(LocalDate.of(2010, Month.JANUARY, 10))
      .build();
  }

  @Test
  void assertMoreThanMaxSizeForCalendar() {
    Assertions.assertThrows(
      IllegalStateException.class,
      () ->
        CalendarDays
          .of()
          .withPeriodStart(LocalDate.of(2000, Month.JANUARY, 1))
          .withPeriodEnd(LocalDate.of(2011, Month.JANUARY, 10))
          .build()
    );
  }

  static final Stream<Arguments> tcToZonedDateTime = testCases.stream();

  @ParameterizedTest(name = "Verify finding transit day and time from zoned date time.")
  @VariableSource("tcToZonedDateTime")
  void timeForDayAndOffset(String text, ZonedDateTime time, int day, int seconds) {
    // "Time Zero" is 04:00  1. Jan 2022
    Assertions.assertEquals(text, subject.time(day, seconds).format(ISO_DATE_TIME));
  }

  static final Stream<Arguments> tcParseTime = testCases.stream();

  @Disabled
  @ParameterizedTest(name = "Verify finding transit day and time from zoned date time.")
  @VariableSource("tcParseTime")
  void transitTime(String text, ZonedDateTime time, Integer day, Integer seconds) {
    /*
      TODO RTM - Fix

    var v = subject.time(time);

    if (v.day() == day) {
      assertEquals(v.timeSec(), seconds, text);
    } else if (seconds > D24h) {
      assertEquals(v.day(), day + 1, text);
      assertEquals(v.timeSec() + D24h, seconds, text);
    } else {
      fail();
    }
    */
  }

  static Arguments tc(String text, int day, int seconds) {
    return Arguments.of(text, ZonedDateTime.parse(text), day, seconds);
  }
}
