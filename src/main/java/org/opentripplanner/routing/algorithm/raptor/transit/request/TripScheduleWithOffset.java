package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.Objects;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.time.LocalDate;

/**
 * This represents a single trip within a TripPattern, but with a time offset in seconds. This is used to represent
 * a trip on a subsequent service day than the first one in the date range used.
 */

public class TripScheduleWithOffset implements TripSchedule {

    private final int secondsOffset;
    private final TripPatternForDates pattern;
    private final TripTimes tripTimes;
    private final LocalDate serviceDate;

    TripScheduleWithOffset(TripPatternForDates pattern, LocalDate localDate, TripTimes tripTimes, int offset) {
        this.pattern = pattern;
        this.tripTimes = tripTimes;
        this.secondsOffset = offset;
        this.serviceDate = localDate;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return this.tripTimes.getArrivalTime(stopPosInPattern) + secondsOffset;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return this.tripTimes.getDepartureTime(stopPosInPattern) + secondsOffset;
    }

    @Override
    public RaptorTripPattern<TripSchedule> pattern() {
        return pattern;
    }

    @Override
    public TripTimes getOriginalTripTimes() {
        return this.tripTimes;
    }

    @Override
    public TripPattern getOriginalTripPattern() {
        return pattern.getTripPattern().getPattern();
    }

    @Override
    public LocalDate getServiceDate() {
        return serviceDate;
    }

    @Override
    public int findStopPosInPattern(int stopIndex, int time, boolean departure) {
        for (int i=0; i < pattern.numberOfStopsInPattern(); ++i) {
            if(pattern.stopIndex(i) != stopIndex) { continue; }
            int t = departure ? departure(i) : arrival(i);
            if(t == time) { return i; }
        }
        throw new IllegalStateException(
            "No stop position(index) in pattern found. StopIndex=" + stopIndex + ", time=" + time +
                ", departure=" + departure + "."
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof TripScheduleWithOffset)) { return false; }
        final TripScheduleWithOffset that = (TripScheduleWithOffset) o;
        return secondsOffset == that.secondsOffset && Objects.equals(tripTimes, that.tripTimes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secondsOffset, tripTimes);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripScheduleWithOffset.class)
                .addObj("trip", pattern.debugInfo())
                .addServiceTime("depart", secondsOffset + tripTimes.getDepartureTime(0))
                .toString();
    }

    public int getSecondsOffset() {
        return secondsOffset;
    }
}
