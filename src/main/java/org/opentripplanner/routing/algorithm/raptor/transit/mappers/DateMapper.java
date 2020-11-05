package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class DateMapper {
    public static ZonedDateTime asStartOfService(ZonedDateTime date) {
        return date.truncatedTo(ChronoUnit.HOURS)
                .withHour(12)
                .minusHours(12);
    }

    public static ZonedDateTime asStartOfService(LocalDate localDate, ZoneId zoneId) {
        return ZonedDateTime.of(localDate, LocalTime.NOON, zoneId)
                .minusHours(12);
    }

    public static int secondsSinceStartOfTime(ZonedDateTime startOfTime, LocalDate localDate) {
        ZonedDateTime startOfDay = asStartOfService(localDate, startOfTime.getZone());
        return (int) Duration.between(startOfTime, startOfDay).getSeconds();
    }

    public static int secondsSinceStartOfTime(ZonedDateTime startOfTime, Instant instant) {
        return (int) Duration.between(startOfTime.toInstant(), instant).getSeconds();
    }

    public static LocalDateTime asDateTime(LocalDate localDate, int secondsSinceStartOfDay) {
        return localDate.atStartOfDay().plusSeconds(secondsSinceStartOfDay);
    }
}
