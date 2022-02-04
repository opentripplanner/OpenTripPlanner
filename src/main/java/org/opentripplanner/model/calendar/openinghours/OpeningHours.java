package org.opentripplanner.model.calendar.openinghours;

import java.time.LocalTime;
import java.util.BitSet;
import java.util.Objects;
import org.opentripplanner.util.time.TimeUtils;

public class OpeningHours {
    private final int startTime;
    private final int endTime;
    private final BitSet days;

    OpeningHours(LocalTime startTime, LocalTime endTime, BitSet days) {
        this.startTime = startTime.toSecondOfDay();
        this.endTime = endTime.toSecondOfDay();
        this.days = days;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof OpeningHours)) {return false;}
        final OpeningHours that = (OpeningHours) o;
        return startTime == that.startTime && endTime == that.endTime && days.equals(that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, days);
    }

    @Override
    public String toString() {
        return "[" + TimeUtils.timeToStrCompact(startTime) +
                " - " + TimeUtils.timeToStrCompact(endTime) +
                "] on " + days.cardinality() + " days";
    }
}
