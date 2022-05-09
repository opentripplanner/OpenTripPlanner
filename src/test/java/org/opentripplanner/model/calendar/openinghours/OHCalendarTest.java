package org.opentripplanner.model.calendar.openinghours;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.trippattern.Deduplicator;

class OHCalendarTest {

  private final ZoneId zoneId = ZoneId.of("Europe/Paris");

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
    calBuilder.openingHours("17th May", time(22, 0), time(23, 59)).on(date(Month.MAY, 17)).add();
    calBuilder.openingHours("18th May", time(0, 0), time(2, 0)).on(date(Month.MAY, 17)).add();

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
      "openingHours: [18th May 00:00-02:00, OCT-25 01:00-03:00, " +
      "1-3. April 08:00-16:30, 17th May 22:00-23:59]" +
      "}",
      c.toString()
    );

    ///////// ROUTING SEARCH  /////////

    // The start of the search, this is used to optimize the calculation
    Instant dateTime = Instant.parse("2022-10-25T01:30:00Z");
    long time = dateTime.getEpochSecond();

    // The context is used to cache calculations for a search, use negative
    // duration for arriveBy search
    var ctx = new OHSearchContext(dateTime, Duration.ofHours(36));

    // Is open at specific time?
    boolean ok = ctx.isOpen(c, time - 1234);

    // Open in hole period from enter to exit?
    boolean okToEnter = ctx.canEnter(c, time + 1234);
    boolean okToExit = ctx.canExit(c, time + 2345);
  }

  private static LocalDate date(Month march, int i) {
    return LocalDate.of(2022, march, i);
  }

  private static LocalTime time(int hour, int min) {
    return LocalTime.of(hour, min);
  }
}
