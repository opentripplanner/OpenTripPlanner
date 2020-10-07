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

    public static final FeedScopedId TP_ID_1 = new FeedScopedId("F","1");
    public static final FeedScopedId TP_ID_2 = new FeedScopedId("F","2");
    public static final FeedScopedId TP_ID_3 = new FeedScopedId("F","3");

    @Test
    public void testMergeTripPatterns() {
        TripTimes[] times = new TripTimes[] { null };

        LocalDate first = LocalDate.of(2019, 3, 30);
        LocalDate second = LocalDate.of(2019, 3, 31);
        LocalDate third = LocalDate.of(2019, 4, 1);

        ZonedDateTime startOfTime = DateMapper.asStartOfService(second, ZoneId.of("Europe/London"));

        // Total available trip patterns
        TripPatternWithRaptorStopIndexes tripPattern1 = new TripPatternWithId(TP_ID_1,null, null);
        TripPatternWithRaptorStopIndexes tripPattern2 = new TripPatternWithId(TP_ID_2,null, null);
        TripPatternWithRaptorStopIndexes tripPattern3 = new TripPatternWithId(TP_ID_3,null, null);

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

        // Get the results
        var r1 = findTripPatternForDate(TP_ID_1, combinedTripPatterns);
        var r2 = findTripPatternForDate(TP_ID_2, combinedTripPatterns);
        var r3 = findTripPatternForDate(TP_ID_3, combinedTripPatterns);


        // Check the number of trip schedules available for each pattern after combining dates in the search range
        assertEquals(2, r1.numberOfTripSchedules());
        assertEquals(2, r2.numberOfTripSchedules());
        assertEquals(3, r3.numberOfTripSchedules());

        // Verify that the per-day offsets were calculated correctly
        //   DST - Clocks go forward on March 31st
        assertEquals(-82800, ((TripScheduleWithOffset) r3.getTripSchedule(0)).getSecondsOffset());
        assertEquals(0, ((TripScheduleWithOffset) r3.getTripSchedule(1)).getSecondsOffset());
        assertEquals(86400, ((TripScheduleWithOffset) r3.getTripSchedule(2)).getSecondsOffset());
    }

    private static TripPatternForDates findTripPatternForDate(FeedScopedId patternId, List<TripPatternForDates> list) {
        return list.stream()
            .filter(p -> patternId.equals(p.getTripPattern().getId()))
            .findFirst()
            .orElseThrow();
    }
}
