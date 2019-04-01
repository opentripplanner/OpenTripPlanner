package org.opentripplanner.routing.algorithm.raptor.transit_data_provider;

import com.conveyal.r5.otp2.api.transit.TripPatternInfo;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TripPattern;

import java.util.List;

/**
 * A collection of all the TripSchedules active on a range of consecutive days. The outer list of tripSchedules
 * refers to days in order.
 */

public class TripPatternForDates implements TripPatternInfo<TripSchedule> {
    private final TripPattern tripPattern;
    private final List<List<TripSchedule>> tripSchedules;
    private static final int SECONDS_OF_DAY = 86400;
    private final int numberOfTripPatterns;

    public TripPatternForDates(TripPattern tripPattern, List<List<TripSchedule>> tripSchedulesPerDay) {
        this.tripPattern = tripPattern;
        this.tripSchedules = tripSchedulesPerDay;
        this.numberOfTripPatterns = tripSchedules.stream().mapToInt(t -> t.size()).sum();
    }

    public TripPattern getTripPattern() {
        return tripPattern;
    }

    @Override
    public int stopIndex(int i) {
        return this.tripPattern.getStopPattern()[i];
    }

    @Override
    public int numberOfStopsInPattern() {
        return tripPattern.getStopPattern().length;
    }

    public List<List<TripSchedule>> getTripSchedules() { return this.tripSchedules; }

    @Override
    public TripSchedule getTripSchedule(int i) {
        int dayOffset = -1; // Start at yesterday to account for trips that cross midnight.
        for (List<TripSchedule> tripScheduleList : tripSchedules ) {
            if (i < tripScheduleList.size()) {
                return new TripScheduleWithOffset(tripScheduleList.get(i), dayOffset * SECONDS_OF_DAY);
            }
            i -= tripScheduleList.size();
            dayOffset++;
        }
        throw new IndexOutOfBoundsException("Index out of bound: " + i);
    }

    @Override
    public int numberOfTripSchedules() {
        return numberOfTripPatterns;
    }
}
