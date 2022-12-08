package org.opentripplanner.model.calendar.openinghours;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model.framework.Deduplicator;

class OHCalendarTest {

  private final ZoneId zoneId = ZoneIds.PARIS;

  @Test
  void simpleCase() {
    ///////// GRAPH BUILD  /////////

    // Create a new service (one instance for the entire graph build) with
    // first and last day of service
    var service = new OpeningHoursCalendarService(
      new Deduplicator(),
      LocalDate.of(2022, Month.JANUARY, 1),
      LocalDate.of(2024, Month.DECEMBER, 31)
    );

    // Create a OHCalendarBuilder for each entity with opening hours
    var calBuilder = service.newBuilder(zoneId);

    // Simple case 08:00- 16:30  April 1st to April 3rd
    calBuilder
      .openingHours("1-3. April", time(8, 0), time(16, 30))
      .on(date(Month.APRIL, 1))
      .on(date(Month.APRIL, 2))
      .on(date(Month.APRIL, 3))
      .add();

    // DST is 27. March - What should we do with th hours from 02:00 to 03:00 (apair twice)
    calBuilder.openingHours("OCT-25", time(1, 0), time(3, 0)).on(date(Month.OCTOBER, 25)).add();

    //  May 17th 22:00 - May 18th 03:00
    calBuilder.openingHours("17th May", time(22, 0), LocalTime.MAX).on(date(Month.MAY, 17)).add();
    calBuilder.openingHours("18th May", LocalTime.MIN, time(2, 0)).on(date(Month.MAY, 17)).add();

    //  Possibilities for the adding date methods:
    //  - on(LocalDate)
    //  - on(DayOfWeek)
    //  - everyDay()
    //  - except(LocalDate)
    //  - except(DayOfWeek)
    // The methods will be applied in the order they are called...

    // Lightweight calendar with all the opening hours above is created and can
    // be assigned to entity
    var c = calBuilder.build();

    assertEquals(
      "OHCalendar{" +
      "zoneId: Europe/Paris, " +
      "openingHours: [18th May 0:00-2:00, OCT-25 1:00-3:00, " +
      "1-3. April 8:00-16:30, 17th May 22:00-23:59:59]" +
      "}",
      c.toString()
    );

    ///////// ROUTING SEARCH  /////////

    // The start of the search, this is used to optimize the calculation
    Instant dateTime = Instant.parse("2022-10-25T00:30:00Z");
    long time = dateTime.getEpochSecond();

    // The context is used to cache calculations for a search, use negative
    // duration for arriveBy search
    // TODO currently doesn't cache
    var ctx = new OHSearchContext(dateTime, Duration.ofHours(36));

    // Is open at specific time?
    boolean open = ctx.isOpen(c, time);
    assertTrue(open);

    // Open in whole period from enter to exit?
    // TODO canEnter and canExit methods don't currently differ from isOpen. Should they?
    boolean okToEnter = ctx.canEnter(c, time);
    assertTrue(okToEnter);
    int thirtyMinutes = 30 * 60;
    boolean okToExit = ctx.canExit(c, time + thirtyMinutes);
    assertTrue(okToExit);

    // Should not be open outside of opening hours
    int fiveHours = 5 * 60 * 60;
    open = ctx.isOpen(c, time - fiveHours);
    assertFalse(open);
    open = ctx.isOpen(c, time + fiveHours);
    assertFalse(open);

    // Should not be open on a day that has no opening hours
    dateTime = Instant.parse("2022-10-23T00:30:00Z");
    ctx = new OHSearchContext(dateTime, Duration.ofHours(36));
    time = dateTime.getEpochSecond();
    open = ctx.isOpen(c, time);
    assertFalse(open);
  }

  @Test
  void openOnMondaysAndSundays() {
    var service = new OpeningHoursCalendarService(
      new Deduplicator(),
      LocalDate.of(2022, Month.MARCH, 1),
      LocalDate.of(2024, Month.JANUARY, 15)
    );

    var calBuilder = service.newBuilder(zoneId);

    calBuilder
      .openingHours("Mondays and Sundays", time(13, 0), time(17, 0))
      .on(DayOfWeek.MONDAY)
      .on(DayOfWeek.SATURDAY)
      .add();

    var c = calBuilder.build();

    assertEquals(
      "OHCalendar{" + "zoneId: Europe/Paris, " + "openingHours: [Mondays and Sundays 13:00-17:00]}",
      c.toString()
    );

    // The start of the search, this is used to optimize the calculation
    // The chosen date is a Monday
    Instant dateTime = Instant.parse("2022-10-24T00:30:00Z");
    long time = dateTime.getEpochSecond();
    var ctx = new OHSearchContext(dateTime, Duration.ofHours(36));

    // Should be open within defined opening hours
    int twelveHours = 12 * 60 * 60;
    boolean open = ctx.isOpen(c, time + twelveHours);
    assertTrue(open);

    // Should not be open outside of opening hours
    open = ctx.isOpen(c, time);
    assertFalse(open);

    // The start of the search, this is used to optimize the calculation
    // The chosen date is a Saturday
    dateTime = Instant.parse("2022-10-29T00:30:00Z");
    time = dateTime.getEpochSecond();

    // The context is used to cache calculations for a search, use negative
    // duration for arriveBy search
    ctx = new OHSearchContext(dateTime, Duration.ofHours(36));

    // Should be open within defined opening hours
    open = ctx.isOpen(c, time + twelveHours);
    assertTrue(open);

    // Should not be open outside of opening hours
    open = ctx.isOpen(c, time);
    assertFalse(open);

    // Should not be open on a day that has no opening hours
    dateTime = Instant.parse("2022-10-30T00:30:00Z");
    time = dateTime.getEpochSecond();
    open = ctx.isOpen(c, time);
    assertFalse(open);
  }

  private static LocalDate date(Month march, int i) {
    return LocalDate.of(2022, march, i);
  }

  private static LocalTime time(int hour, int min) {
    return LocalTime.of(hour, min);
  }
}
