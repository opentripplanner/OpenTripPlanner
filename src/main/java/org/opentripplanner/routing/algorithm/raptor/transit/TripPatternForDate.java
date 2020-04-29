package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A TripPattern with its TripSchedules filtered by validity on a particular date. This is to avoid
 * having to do any filtering by date during the search itself.
 */
public class TripPatternForDate {

    /**
     * The original TripPattern whose TripSchedules were filtered to produce this.tripSchedules.
     * Its TripSchedules remain unchanged.
     */
    private final TripPatternWithRaptorStopIndexes tripPattern;

    /**
     * The filtered TripSchedules for only those trips in the TripPattern that are active on the given day.
     * Invariant: this array should contain a subset of the TripSchedules in tripPattern.tripSchedules.
     */
    private final TripTimes[] tripTimes;

    /** The date for which the filtering was performed. */
    private final LocalDate localDate;

    public TripPatternForDate(TripPatternWithRaptorStopIndexes tripPattern, TripTimes[] tripTimes, LocalDate localDate) {
        this.tripPattern = tripPattern;
        this.tripTimes = tripTimes;
        this.localDate = localDate;
    }

    public TripTimes[] tripTimes() {
        return tripTimes;
    }

    public TripPatternWithRaptorStopIndexes getTripPattern() {
        return tripPattern;
    }

    public int stopIndex(int i) {
        return this.tripPattern.stopIndex(i);
    }

    public TripTimes getTripTimes(int i) {
        return tripTimes[i];
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public int numberOfTripSchedules() {
        return tripTimes.length;
    }

    public int hashCode() {
        return Objects.hash(tripPattern, tripTimes, localDate);
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TripPatternForDate that = (TripPatternForDate) o;

        return tripPattern.equals(that.tripPattern) && localDate.equals(that.localDate);
    }

    @Override
    public String toString() {
        return "TripPatternForDate{" +
                "tripPattern=" + tripPattern +
                ", localDate=" + localDate +
                '}';
    }
}
