package org.opentripplanner.model.calendar.openinghours;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.opentripplanner.routing.trippattern.Deduplicator;

public class OHCalendarBuilder {

    private final Deduplicator deduplicator;
    private final LocalDate startOfPeriod;
    private final int daysInPeriod;
    private final List<OpeningHours> openingHours = new ArrayList<>();

    public OHCalendarBuilder(Deduplicator deduplicator, LocalDate startOfPeriod, int daysInPeriod) {
        this.deduplicator = deduplicator;
        this.startOfPeriod = startOfPeriod;
        this.daysInPeriod = daysInPeriod;
    }

    public OpeningHoursBuilder withHours(LocalTime startTime, LocalTime endTime) {
        return new OpeningHoursBuilder(startTime, endTime);
    }

    public OHCalendar build() {
        return new OHCalendar(openingHours);
    }

    private int dayIndex(LocalDate date) {
        return Period.between(startOfPeriod, date).getDays();
    }

    class OpeningHoursBuilder {
        private final LocalTime startTime;
        private final LocalTime endTime;

        private final BitSet openingDays = new BitSet(daysInPeriod);

        public OpeningHoursBuilder(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public OpeningHoursBuilder onDate(LocalDate date) {
            openingDays.set(dayIndex(date));
            return this;
        }
        public OHCalendarBuilder add() {
            var days = deduplicator.deduplicateBitSet(openingDays);
            var hours = deduplicator.deduplicateObject(
                    OpeningHours.class,
                    new OpeningHours(startTime, endTime, days)
            ) ;
            openingHours.add(hours);
            return OHCalendarBuilder.this;
        }
    }
}
