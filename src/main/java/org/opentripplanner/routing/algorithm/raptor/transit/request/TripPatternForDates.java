package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Arrays;
import java.util.List;

/**
 * A collection of all the TripSchedules active on a range of consecutive days. The outer list of tripSchedulesByDay
 * refers to days in order.
 */
public class TripPatternForDates implements RaptorRoute<TripSchedule>,
        RaptorTimeTable<TripSchedule>, RaptorTripPattern {

    private final TripPatternWithRaptorStopIndexes tripPattern;

    private final TripPatternForDate[] tripPatternForDates;

    private final int[] offsets;

    private final int numberOfTripSchedules;

    TripPatternForDates(TripPatternWithRaptorStopIndexes tripPattern, List<TripPatternForDate> tripPatternForDates, List<Integer> offsets) {
        this.tripPattern = tripPattern;
        this.tripPatternForDates = tripPatternForDates.toArray(new TripPatternForDate[]{});
        this.offsets = offsets.stream().mapToInt(i -> i).toArray();
        this.numberOfTripSchedules = Arrays.stream(this.tripPatternForDates).mapToInt(TripPatternForDate::numberOfTripSchedules).sum();
    }

    public TripPatternWithRaptorStopIndexes getTripPattern() {
        return tripPattern;
    }


    // Implementing RaptorRoute
    @Override
    public RaptorTimeTable<TripSchedule> timetable() {
        return this;
    }

    @Override
    public RaptorTripPattern pattern() {
        return this;
    }

    // Implementing RaptorTripPattern

    @Override public int stopIndex(int stopPositionInPattern) {
        return tripPattern.stopIndex(stopPositionInPattern);
    }

    @Override
    public boolean boardingPossibleAt(int stopPositionInPattern) {
        return tripPattern.getPattern().canBoard(stopPositionInPattern);
    }

    @Override
    public boolean alightingPossibleAt(int stopPositionInPattern) {
        return tripPattern.getPattern().canAlight(stopPositionInPattern);
    }

    @Override public int numberOfStopsInPattern() {
        return tripPattern.getStopIndexes().length;
    }

    @Override
    public String modeInfo() { return tripPattern.getTransitMode().name(); }

    // Implementing RaptorTimeTable

    @Override public TripSchedule getTripSchedule(int index) {
        for (int i = 0; i < tripPatternForDates.length; i++) {
            TripPatternForDate tripPatternForDate = tripPatternForDates[i];

            if (index < tripPatternForDate.numberOfTripSchedules()) {
                return new TripScheduleWithOffset(this, tripPatternForDate.getTripTimes(index), offsets[i]);
            }
            index -= tripPatternForDate.numberOfTripSchedules();
        }
        throw new IndexOutOfBoundsException("Index out of bound: " + index);
    }

    @Override public int numberOfTripSchedules() {
        return numberOfTripSchedules;
    }
}
