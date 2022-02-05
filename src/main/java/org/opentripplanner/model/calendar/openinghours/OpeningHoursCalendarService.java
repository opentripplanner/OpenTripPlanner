package org.opentripplanner.model.calendar.openinghours;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import org.opentripplanner.routing.trippattern.Deduplicator;

public class OpeningHoursCalendarService {

    private final Deduplicator deduplicator;
    private final LocalDate startOfPeriod;
    private final int daysInPeriod;

    public OpeningHoursCalendarService(
            Deduplicator deduplicator,
            LocalDate startOfPeriod,
            LocalDate endOfPeriod
    ) {
        this.deduplicator = deduplicator;
        this.startOfPeriod = startOfPeriod;
        this.daysInPeriod = Period.between(startOfPeriod, endOfPeriod).getDays();
    }

    public OHCalendarBuilder newBuilder(ZoneId zoneId) {
        return new OHCalendarBuilder(deduplicator, startOfPeriod, daysInPeriod, zoneId);
    }
}
