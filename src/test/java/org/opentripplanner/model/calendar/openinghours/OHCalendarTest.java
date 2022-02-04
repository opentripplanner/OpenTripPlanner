package org.opentripplanner.model.calendar.openinghours;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
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
        var cBuilder = service.newBuilder(zoneId);

        // Simple case 08:00- 16:30  April 1st to April 3rd
        cBuilder.withHours(time(8,0), time(16,30))
                .onDate(date(Month.APRIL, 1))
                .onDate(date(Month.APRIL, 2))
                .onDate(date(Month.APRIL, 3))
                .add();

        // DST is 27. March - What should we do with th hours from 02:00 to 03:00 (apair twice)
        cBuilder.withHours(time(1,0), time(3,0))
                .onDate(date(Month.OCTOBER, 25))
                .add();

        //  May 17th 22:00 - May 18th 03:00
        cBuilder.withHours(time(22,0), time(27,0))
                .onDate(date(Month.MAY, 17))
                .add();

        // Lightweight calendar with all the opening hours above is created and can
        // be assigned to entity
        var c = cBuilder.build();


        ///////// ROUTING SEARCH  /////////

        // The start of the search, this is used to optimize the calculation
        Instant dateTime = Instant.parse("2022-10-25T03:30Z");
        long time = dateTime.getEpochSecond();

        // The context is used to cache calculations for a search
        var ctx = new OHSearchContext(dateTime);

        // Is open at specific time?
        boolean ok = ctx.isOpen(c, time - 1234);

        // Open in hole period from enter to exit?
        boolean okToEnter = ctx.enterOk(c, time + 1234);
        boolean okToExit = ctx.exitOk(c, time + 2345);
    }

    private static LocalDate date(Month march, int i) {
        return LocalDate.of(2022, march, i);
    }

    private static LocalTime time(int hour, int  min) {
        return LocalTime.of(hour, min);
    }
}