package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * This class merges several a list of TripPatterns for several consecutive dates into a single
 * list of TripPatternsForDates. The merge-method assumes that the lists are already sorted by
 * TripPattern. The purpose of doing this is so that TripSchedules for several dates are combined
 * by TripPattern instead of having their own TripPattern. This is to improve performance for
 * searching, as each TripPattern is searched only once per round.
 */

//TODO This needs to be fixed and tests added
public class MergeTripPatternForDates {

    public static List<TripPatternForDates> merge(List<List<TripPatternForDate>> tripPatternForDateList) {
        List<TripPatternForDates> tripPatternForDates = new ArrayList<>();

        for (List<TripPatternForDate> it : tripPatternForDateList) {
            tripPatternForDates = mergeSingleList(tripPatternForDates, it);
        }

        return tripPatternForDates;
    }

    private static List<TripPatternForDates> mergeSingleList(List<TripPatternForDates> left, List<TripPatternForDate> right) {
        List<TripPatternForDates> combinedList = new ArrayList<>();

        int dayOffset = left.isEmpty() ? 0 : left.get(0).getTripSchedules().size();

        int leftIndex = 0;
        int rightIndex = 0;

        while (leftIndex < left.size() && rightIndex < right.size()) {
            TripPatternForDates leftItem = left.get(leftIndex);
            TripPatternForDate rightItem = right.get(rightIndex);

            int compareResult = Integer.compare(leftItem.getTripPattern().getId(), rightItem.getTripPattern().getId());

            if (compareResult == 0) {
                combinedList.add(combine(left.get(leftIndex), right.get(rightIndex)));
                leftIndex++;
                rightIndex++;
            } else if (compareResult > 0 ) {
                combinedList.add(combine(left.get(leftIndex), 1));
                leftIndex++;
            } else {
                combinedList.add(combine(dayOffset, right.get(rightIndex)));
                rightIndex++;
            }
        }

        for(;leftIndex<left.size();++leftIndex) {
            combinedList.add(combine(left.get(leftIndex), 1));
        }
        for(;rightIndex<right.size();++rightIndex) {
            combinedList.add(combine(dayOffset, right.get(rightIndex)));
        }

        return combinedList;
    }

    private static TripPatternForDates combine(TripPatternForDates tripPatternForDates, TripPatternForDate tripPatternForDate) {
        tripPatternForDates.getTripSchedules().add(tripPatternForDate.getTripSchedules());
        return tripPatternForDates;
    }

    private static TripPatternForDates combine(int dayOffset, TripPatternForDate tripPatternForDate) {
        List<List<TripSchedule>> tripSchedulesList = new ArrayList<>();
        for (int i = 0; i < dayOffset; i++) {
            tripSchedulesList.add(new ArrayList<>());
        }
        tripSchedulesList.add(tripPatternForDate.getTripSchedules());
        return new TripPatternForDates(tripPatternForDate.getTripPattern(), tripSchedulesList);
    }

    private static TripPatternForDates combine(TripPatternForDates tripPatternForDates, int dayOffset) {
        for (int i = 0; i < dayOffset; i++) {
            tripPatternForDates.getTripSchedules().add(new ArrayList<>());
        }
        return tripPatternForDates;
    }
}
