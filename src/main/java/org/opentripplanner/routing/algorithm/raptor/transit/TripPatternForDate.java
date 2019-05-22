package org.opentripplanner.routing.algorithm.raptor.transit;

import com.conveyal.r5.otp2.api.transit.TripPatternInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A TripPattern with its TripSchedules filtered by validity on a particular date. This is to avoid
 * having to do any filtering by date during the search itself.
 */
public class TripPatternForDate implements TripPatternInfo<TripSchedule> {
    private final TripPattern tripPattern;

    private final TripSchedule[] tripSchedules;

    private final LocalDate localDate;

    public TripPatternForDate(TripPattern tripPattern, List<TripSchedule> tripSchedules, LocalDate localDate) {
        this.tripPattern = tripPattern;
        this.tripSchedules = tripSchedules.toArray(new TripSchedule[]{});
        this.localDate = localDate;
    }

    public TripPattern getTripPattern() {
        return tripPattern;
    }

    @Override public int stopIndex(int i) {
        return this.tripPattern.stopIndex(i);
    }

    @Override public int numberOfStopsInPattern() {
        return tripPattern.getStopIndexes().length;
    }

    @Override public TripSchedule getTripSchedule(int i) {
        return tripSchedules[i];
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    @Override public int numberOfTripSchedules() {
        return tripSchedules.length;
    }

    @Override public int hashCode() {
        return Objects.hash(tripPattern, tripSchedules, localDate);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TripPatternForDate that = (TripPatternForDate) o;
        return this.getTripPattern().getId() == that.getTripPattern().getId();
    }

    @Override
    public String toString() {
        return "TripPatternForDate{" +
                "tripPattern=" + tripPattern +
                ", localDate=" + localDate +
                '}';
    }
}
