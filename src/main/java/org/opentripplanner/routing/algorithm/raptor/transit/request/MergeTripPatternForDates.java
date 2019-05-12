package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper.secondsSinceStartOfTime;

/**
 * This class merges several a list of TripPatterns for several consecutive dates into a single
 * list of TripPatternsForDates. The purpose of doing this is so that TripSchedules for several dates
 * are combined by TripPattern instead of having their own TripPattern. This is to improve performance
 * for searching, as each TripPattern is searched only once per round.
 */

//TODO Add test
public class MergeTripPatternForDates {

    public static List<TripPatternForDates> merge(List<Map<Integer, TripPatternForDate>> tripPatternForDateList, ZonedDateTime startOfTime) {
        List<TripPatternForDates> combinedList = new ArrayList<>();

        Map<Integer, TripPattern> allTripPatternsById = tripPatternForDateList.stream().flatMap(t -> t.values().stream())
                .map(TripPatternForDate::getTripPattern)
                .distinct()
                .collect(Collectors.toMap(TripPattern::getId, t -> t));

        for (Map.Entry<Integer, TripPattern> patternEntry : allTripPatternsById.entrySet()) {
            List<TripPatternForDate> tripPatterns = new ArrayList<>();
            List<Integer> offsets = new ArrayList<>();

            for (Map<Integer, TripPatternForDate> tripPatternById : tripPatternForDateList) {
                TripPatternForDate tripPatternForDate = tripPatternById.get(patternEntry.getKey());
                if (tripPatternForDate != null) {
                    tripPatterns.add(tripPatternForDate);
                    offsets.add(secondsSinceStartOfTime(startOfTime, tripPatternForDate.getLocalDate()));
                }
            }

            combinedList.add(new TripPatternForDates(patternEntry.getValue(), tripPatterns, offsets));
        }

        return combinedList;
    }
}
