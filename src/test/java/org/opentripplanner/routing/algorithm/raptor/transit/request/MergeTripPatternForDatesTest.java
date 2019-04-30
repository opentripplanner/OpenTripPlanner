package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.junit.Test;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.TripScheduleImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MergeTripPatternForDatesTest {

    @Test
    public void testMergeTripPatterns() {
        List<TripSchedule> tripSchedules = new ArrayList<>();
        tripSchedules.add(new TripScheduleImpl(null, null, null, null, 0));

        // Total available trip patterns
        TripPattern tripPattern1 = new TripPattern(1, tripSchedules, null, null);
        TripPattern tripPattern2 = new TripPattern(2, tripSchedules, null, null);
        TripPattern tripPattern3 = new TripPattern(3, tripSchedules, null, null);

        List<Map<Integer, TripPatternForDate>> tripPatternsForDates = new ArrayList<>();

        // TripPatterns valid for 1st day in search range
        Map<Integer, TripPatternForDate> tripPatternForDatesById = new HashMap<>();
        tripPatternForDatesById.put(tripPattern1.getId(), new TripPatternForDate(tripPattern1, tripPattern1.getTripSchedules()));
        tripPatternForDatesById.put(tripPattern2.getId(), new TripPatternForDate(tripPattern2, tripPattern2.getTripSchedules()));
        tripPatternForDatesById.put(tripPattern3.getId(), new TripPatternForDate(tripPattern1, tripPattern3.getTripSchedules()));
        tripPatternsForDates.add(tripPatternForDatesById);

        // TripPatterns valid for 2nd day in search range
        Map<Integer, TripPatternForDate> tripPatternForDatesById2 = new HashMap<>();
        tripPatternForDatesById2.put(tripPattern2.getId(), new TripPatternForDate(tripPattern2, tripPattern1.getTripSchedules()));
        tripPatternForDatesById2.put(tripPattern3.getId(), new TripPatternForDate(tripPattern3, tripPattern2.getTripSchedules()));
        tripPatternsForDates.add(tripPatternForDatesById2);

        // TripPatterns valid for 3rd day in search range
        Map<Integer, TripPatternForDate> tripPatternForDatesById3 = new HashMap<>();
        tripPatternForDatesById3.put(tripPattern1.getId(), new TripPatternForDate(tripPattern1, tripPattern1.getTripSchedules()));
        tripPatternForDatesById3.put(tripPattern3.getId(), new TripPatternForDate(tripPattern3, tripPattern3.getTripSchedules()));
        tripPatternsForDates.add(tripPatternForDatesById3);

        // Patterns containing trip schedules for all 3 days. Trip schedules for later days are offset in time when requested.
        List<TripPatternForDates> combinedTripPatterns = MergeTripPatternForDates.merge(tripPatternsForDates);

        // Check the number of trip schedules available for each pattern after combining dates in the search range
        assertEquals(combinedTripPatterns.get(0).numberOfTripSchedules(), 2);
        assertEquals(combinedTripPatterns.get(1).numberOfTripSchedules(), 2);
        assertEquals(combinedTripPatterns.get(2).numberOfTripSchedules(), 3);
    }
}
