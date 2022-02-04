package org.opentripplanner.model.calendar.openinghours;

import java.time.LocalDate;
import java.time.LocalTime;
import org.opentripplanner.routing.trippattern.Deduplicator;

public class OHCalendarBuilder {

    private final Deduplicator deduplicator;

    public OHCalendarBuilder(Deduplicator deduplicator) {

        this.deduplicator = deduplicator;
    }

    public OpeningHoursBuilder withHours(LocalTime startTime, LocalTime endTime) {
        return new OpeningHoursBuilder();
    }

    public OHCalendar build() {
        return new OHCalendar();
    }

    public OHCalendarBuilder add() {
        return this;
    }

    class OpeningHoursBuilder {
        public OpeningHoursBuilder onDate(LocalDate date) {
            return this;
        }

        public OHCalendarBuilder add() {
            return  OHCalendarBuilder.this;
        }
    }
}
