package org.opentripplanner.routing.algorithm.raptor.transit;

import java.time.LocalDate;
import java.util.List;
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
    private final TripPattern tripPattern;

    /**
     * The filtered TripSchedules for only those trips in the TripPattern that are active on the given day.
     * Invariant: this array should contain a subset of the TripSchedules in tripPattern.tripSchedules.
     */
    private final TripScheduleWrapperImpl[] tripSchedules;

    /** The date for which the filtering was performed. */
    private final LocalDate localDate;

    public TripPatternForDate(TripPattern tripPattern, List<TripScheduleWrapperImpl> tripSchedules, LocalDate localDate) {
        this.tripPattern = tripPattern;
        this.tripSchedules = tripSchedules.toArray(new TripScheduleWrapperImpl[]{});
        this.localDate = localDate;
    }

    public TripPattern getTripPattern() {
        return tripPattern;
    }

    public int stopIndex(int i) {
        return this.tripPattern.stopIndex(i);
    }

    public int numberOfStopsInPattern() {
        return tripPattern.getStopIndexes().length;
    }

    public TripScheduleWrapperImpl getTripSchedule(int i) {
        return tripSchedules[i];
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public int numberOfTripSchedules() {
        return tripSchedules.length;
    }

    public int hashCode() {
        return Objects.hash(tripPattern, tripSchedules, localDate);
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
