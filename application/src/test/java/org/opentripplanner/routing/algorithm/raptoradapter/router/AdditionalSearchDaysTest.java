package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;

class AdditionalSearchDaysTest {

  @Test
  void morning() {
    var days = getDays("2022-01-25T05:05:20+01:00", true);
    assertEquals(0, days.additionalSearchDaysInFuture());
    assertEquals(1, days.additionalSearchDaysInPast());
  }

  @Test
  void middleOfDay() {
    var days1 = getDays("2022-01-25T13:14:20+01:00", false);
    assertEquals(0, days1.additionalSearchDaysInFuture());
    assertEquals(0, days1.additionalSearchDaysInPast());

    var days2 = getDays("2022-01-25T13:14:20+01:00", true);
    assertEquals(0, days2.additionalSearchDaysInFuture());
    assertEquals(0, days2.additionalSearchDaysInPast());
  }

  @Test
  void evening() {
    var days = getDays("2022-01-25T20:14:20+01:00", false);
    assertEquals(1, days.additionalSearchDaysInFuture());
    assertEquals(0, days.additionalSearchDaysInPast());

    var days2 = getDays("2022-01-25T20:14:20+01:00", true);
    assertEquals(0, days2.additionalSearchDaysInFuture());
    assertEquals(0, days2.additionalSearchDaysInPast());
  }

  @Test
  void closeToMidnight() {
    var days = getDays("2022-01-25T23:14:20+01:00", false);
    assertEquals(1, days.additionalSearchDaysInFuture());
    assertEquals(0, days.additionalSearchDaysInPast());

    var days2 = getDays("2022-01-25T23:14:20+01:00", true);
    assertEquals(0, days2.additionalSearchDaysInPast());
    assertEquals(0, days2.additionalSearchDaysInFuture());
  }

  @Test
  void shortlyAfterMidnight() {
    var days = getDays("2022-01-25T00:15:25+01:00", false);

    assertEquals(0, days.additionalSearchDaysInPast());
    assertEquals(0, days.additionalSearchDaysInFuture());

    var days2 = getDays("2022-01-25T00:15:25+01:00", true);
    assertEquals(1, days2.additionalSearchDaysInPast());
    assertEquals(0, days2.additionalSearchDaysInFuture());
  }

  @Test
  void veryShortWindows() {
    var time = OffsetDateTime.parse("2022-01-25T00:23:25+01:00").atZoneSameInstant(ZoneIds.BERLIN);

    var days = new AdditionalSearchDays(
      false,
      time,
      null,
      Duration.ofMinutes(20),
      Duration.ofMinutes(1)
    );
    assertEquals(0, days.additionalSearchDaysInPast());
    assertEquals(0, days.additionalSearchDaysInFuture());

    var days2 = new AdditionalSearchDays(
      true,
      time,
      null,
      Duration.ofMinutes(20),
      Duration.ofMinutes(1)
    );

    assertEquals(0, days2.additionalSearchDaysInPast());
    assertEquals(0, days2.additionalSearchDaysInFuture());
  }

  private AdditionalSearchDays getDays(String time, boolean arriveBy) {
    var zonedDateTime = OffsetDateTime.parse(time).atZoneSameInstant(ZoneIds.BERLIN);
    return new AdditionalSearchDays(
      arriveBy,
      zonedDateTime,
      Duration.ofHours(2),
      Duration.ofHours(6),
      Duration.ofHours(6)
    );
  }
}
