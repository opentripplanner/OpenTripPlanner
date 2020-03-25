package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RaptorRoutingRequestTransitDataCreatorTest {

    @Test
    public void testMergeTripPatterns() {
        TripTimes[] times = new TripTimes[] { null };

        LocalDate first = LocalDate.of(2019, 3, 30);
        LocalDate second = LocalDate.of(2019, 3, 31);
        LocalDate third = LocalDate.of(2019, 4, 1);

        ZonedDateTime startOfTime = DateMapper.asStartOfService(second, ZoneId.of("Europe/London"));

        // Total available trip patterns
        TripPatternWithRaptorStopIndexes tripPattern1 = new TripPatternWithId(new FeedScopedId("", "1"),null, null);
        TripPatternWithRaptorStopIndexes tripPattern2 = new TripPatternWithId(new FeedScopedId("", "2"),null, null);
        TripPatternWithRaptorStopIndexes tripPattern3 = new TripPatternWithId(new FeedScopedId("", "3"),null, null);

        List<Map<FeedScopedId, TripPatternForDate>> tripPatternsForDates = new ArrayList<>();

        // TripPatterns valid for 1st day in search range
        Map<FeedScopedId, TripPatternForDate> tripPatternForDatesById = new HashMap<>();
        tripPatternForDatesById.put(tripPattern1.getId(), new TripPatternForDate(tripPattern1, times, first));
        tripPatternForDatesById.put(tripPattern2.getId(), new TripPatternForDate(tripPattern2, times, first));
        tripPatternForDatesById.put(tripPattern3.getId(), new TripPatternForDate(tripPattern1, times, first));
        tripPatternsForDates.add(tripPatternForDatesById);

        // TripPatterns valid for 2nd day in search range
        Map<FeedScopedId, TripPatternForDate> tripPatternForDatesById2 = new HashMap<>();
        tripPatternForDatesById2.put(tripPattern2.getId(), new TripPatternForDate(tripPattern2, times, second));
        tripPatternForDatesById2.put(tripPattern3.getId(), new TripPatternForDate(tripPattern3, times, second));
        tripPatternsForDates.add(tripPatternForDatesById2);

        // TripPatterns valid for 3rd day in search range
        Map<FeedScopedId, TripPatternForDate> tripPatternForDatesById3 = new HashMap<>();
        tripPatternForDatesById3.put(tripPattern1.getId(), new TripPatternForDate(tripPattern1, times, third));
        tripPatternForDatesById3.put(tripPattern3.getId(), new TripPatternForDate(tripPattern3, times, third));
        tripPatternsForDates.add(tripPatternForDatesById3);

        // Patterns containing trip schedules for all 3 days. Trip schedules for later days are offset in time when requested.
        List<TripPatternForDates> combinedTripPatterns = RaptorRoutingRequestTransitDataCreator.merge(
            startOfTime, tripPatternsForDates
        );

        // Check the number of trip schedules available for each pattern after combining dates in the search range
        assertEquals(2, combinedTripPatterns.get(0).numberOfTripSchedules());
        assertEquals(2, combinedTripPatterns.get(1).numberOfTripSchedules());
        assertEquals(3, combinedTripPatterns.get(2).numberOfTripSchedules());

        // Verify that the per-day offsets were calculated correctly
        //   DST - Clocks go forward on March 31st
        assertEquals(-82800, ((TripScheduleWithOffset) combinedTripPatterns.get(2).getTripSchedule(0)).getSecondsOffset());
        assertEquals(0, ((TripScheduleWithOffset) combinedTripPatterns.get(2).getTripSchedule(1)).getSecondsOffset());
        assertEquals(86400, ((TripScheduleWithOffset) combinedTripPatterns.get(2).getTripSchedule(2)).getSecondsOffset());
    }
}
