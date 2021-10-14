package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.trippattern.TripTimes;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A TripPattern with its TripSchedules filtered by validity on a particular date. This is to avoid
 * having to do any filtering by date during the search itself.
 */
public class TripPatternForDate {

    /**
     * The original TripPattern whose TripSchedules were filtered to produce this.tripSchedules. Its
     * TripSchedules remain unchanged.
     */
    private final TripPatternWithRaptorStopIndexes tripPattern;

    /**
     * The filtered TripSchedules for only those trips in the TripPattern that are active on the
     * given day. Invariant: this array should contain a subset of the TripSchedules in
     * tripPattern.tripSchedules.
     */
    private final List<TripTimes> tripTimes;

    /** The date for which the filtering was performed. */
    private final LocalDate localDate;

    /**
     * The first departure time of the first trip.
     */
    private final LocalDateTime startOfRunningPeriod;

    /**
     * The last arrival time of the last trip.
     */
    private final LocalDateTime endOfRunningPeriod;

    public TripPatternForDate(
        TripPatternWithRaptorStopIndexes tripPattern,
        List<TripTimes> tripTimes,
        LocalDate localDate
    ) {
        this.tripPattern = tripPattern;
        this.tripTimes = new ArrayList<>(tripTimes);
        this.localDate = localDate;

        // These depend on the tripTimes array being sorted
        this.startOfRunningPeriod = DateMapper.asDateTime(
            localDate,
            tripTimes.get(0).getDepartureTime(0)
        );
        var last = tripTimes.get(tripTimes.size()-1);
        this.endOfRunningPeriod = DateMapper.asDateTime(
            localDate, last.getArrivalTime(last.getNumStops() - 1)
        );
    }

    public List<TripTimes> tripTimes() {
        return tripTimes;
    }

    public TripPatternWithRaptorStopIndexes getTripPattern() {
        return tripPattern;
    }

    public int stopIndex(int i) {
        return this.tripPattern.stopIndex(i);
    }

    public TripTimes getTripTimes(int i) {
        return tripTimes.get(i);
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public int numberOfTripSchedules() {
        return tripTimes.size();
    }

    public LocalDateTime getStartOfRunningPeriod() {
        return startOfRunningPeriod;
    }

    public List<LocalDate> getRunningPeriodDates() {
        // Add one day to ensure last day is included
        return startOfRunningPeriod
            .toLocalDate()
            .datesUntil(endOfRunningPeriod.toLocalDate().plusDays(1))
            .collect(Collectors.toList());
    }

    public int hashCode() {
        return Objects.hash(tripPattern, tripTimes, localDate);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TripPatternForDate that = (TripPatternForDate) o;

        return tripPattern.equals(that.tripPattern) &&
            localDate.equals(that.localDate) &&
            tripTimes.equals(that.tripTimes);
    }

    @Nullable
    public TripPatternForDate newWithFilteredTripTimes(Predicate<TripTimes> filter) {
        List<TripTimes> filteredTripTimes = tripTimes
            .stream()
            .filter(filter)
            .collect(Collectors.toList());

        if (filteredTripTimes.isEmpty()) { return null; }

        if (tripTimes.size() == filteredTripTimes.size()) { return this; }

        return new TripPatternForDate(
            tripPattern,
            filteredTripTimes,
            localDate
        );
    }

    @Override
    public String toString() {
        return "TripPatternForDate{" +
                "tripPattern=" + tripPattern +
                ", localDate=" + localDate +
                '}';
    }
}
