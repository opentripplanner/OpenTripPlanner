package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class merges several a list of TripPatterns for several consecutive dates into a single
 * list of TripPatternsForDates. The purpose of doing this is so that TripSchedules for several dates
 * are combined by TripPattern instead of having their own TripPattern. This is to improve performance
 * for searching, as each TripPattern is searched only once per round.
 */

//TODO Add test
public class MergeTripPatternForDates {

    public static List<TripPatternForDates> merge(List<Map<Integer, TripPatternForDate>> tripPatternForDateList) {
        List<TripPatternForDates> combinedList = new ArrayList<>();

        Map<Integer, TripPattern> allTripPatternsById = tripPatternForDateList.stream().flatMap(t -> t.values().stream())
                .distinct()
                .collect(Collectors.toMap(t -> t.getTripPattern().getId(), TripPatternForDate::getTripPattern));

        for (Map.Entry<Integer, TripPattern> patternEntry : allTripPatternsById.entrySet()) {
            List<List<TripSchedule>> tripSchedulesList = new ArrayList<>();

            for (Map<Integer, TripPatternForDate> tripPatternById : tripPatternForDateList) {
                TripPatternForDate tripPatternForDate = tripPatternById.get(patternEntry.getKey());
                tripSchedulesList.add(
                        tripPatternForDate == null ?
                                new ArrayList<>() :
                                tripPatternForDate.getTripSchedules());
            }

            combinedList.add(new TripPatternForDates(patternEntry.getValue(), tripSchedulesList));
        }

        return combinedList;
    }
}