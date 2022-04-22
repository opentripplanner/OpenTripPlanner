package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper.secondsSinceStartOfTime;

import gnu.trove.list.array.TIntArrayList;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;

/**
 * This class is responsible for creating the internal data structure of {@link
 * RaptorRoutingRequestTransitData}. The code is messy so it is nice to NOT have it in the transit
 * data class itself, to keep it clean. Another benefit of isolating this code is that these methods
 * are only available at construction time.
 */
class RaptorRoutingRequestTransitDataCreator {

  private final TransitLayer transitLayer;
  private final ZonedDateTime transitSearchTimeZero;
  private final LocalDate departureDate;

  RaptorRoutingRequestTransitDataCreator(
    TransitLayer transitLayer,
    ZonedDateTime transitSearchTimeZero
  ) {
    this.transitLayer = transitLayer;
    this.departureDate = transitSearchTimeZero.toLocalDate();
    this.transitSearchTimeZero = transitSearchTimeZero;
  }

  public List<int[]> createTripPatternsPerStop(List<TripPatternForDates> tripPatternsForDate) {
    // Create temporary array of TIntArrayLists
    int stopCount = transitLayer.getStopCount();
    TIntArrayList[] patternsForStop = new TIntArrayList[stopCount];
    for (int i = 0; i < stopCount; i++) {
      patternsForStop[i] = new TIntArrayList();
    }

    // Loop through all patterns, and mark all stops containing that pattern
    int numPatterns = tripPatternsForDate.size();
    for (int patternIndex = 0; patternIndex < numPatterns; patternIndex++) {
      TripPatternForDates tripPatternForDateList = tripPatternsForDate.get(patternIndex);
      for (int i : tripPatternForDateList.getTripPattern().getStopIndexes()) {
        patternsForStop[i].add(patternIndex);
      }
    }

    // Create the final list with raw int arrays, in order to get best iteration performance
    List<int[]> result = new ArrayList<>();
    for (var patterns : patternsForStop) {
      result.add(patterns.toArray());
    }

    return result;
  }

  /**
   * This method merges several list of TripPatterns for several consecutive dates into a single
   * list of TripPatternsForDates. The purpose of doing this is so that TripSchedules for several
   * dates are combined by TripPattern instead of having their own TripPattern. This is to improve
   * performance for searching, as each TripPattern is searched only once per round.
   */
  static List<TripPatternForDates> merge(
    ZonedDateTime transitSearchTimeZero,
    List<TripPatternForDate> patternForDateList,
    TransitDataProviderFilter filter
  ) {
    // Group TripPatternForDate objects by TripPattern.
    // This is done in a loop to increase performance.
    Map<TripPatternWithRaptorStopIndexes, List<TripPatternForDate>> patternForDateByPattern = new HashMap<>();
    for (TripPatternForDate patternForDate : patternForDateList) {
      patternForDateByPattern
        .computeIfAbsent(patternForDate.getTripPattern(), k -> new ArrayList<>())
        .add(patternForDate);
    }

    List<TripPatternForDates> combinedList = new ArrayList<>();

    // For each TripPattern, time expand each TripPatternForDate and merge into a single
    // TripPatternForDates
    for (Map.Entry<TripPatternWithRaptorStopIndexes, List<TripPatternForDate>> patternEntry : patternForDateByPattern.entrySet()) {
      // Sort by date. We can mutate the array, as it was created above in the grouping.
      List<TripPatternForDate> patternsSorted = patternEntry.getValue();
      patternsSorted.sort(Comparator.comparing(TripPatternForDate::getLocalDate));

      // Calculate offsets per date
      List<Integer> offsets = new ArrayList<>();
      for (TripPatternForDate tripPatternForDate : patternsSorted) {
        offsets.add(
          secondsSinceStartOfTime(transitSearchTimeZero, tripPatternForDate.getLocalDate())
        );
      }

      // Combine TripPatternForDate objects
      final TripPatternWithRaptorStopIndexes tripPattern = patternEntry.getKey();

      combinedList.add(
        new TripPatternForDates(
          tripPattern,
          patternsSorted,
          offsets,
          filter.filterAvailableStops(tripPattern, tripPattern.getBoardingPossible()),
          filter.filterAvailableStops(tripPattern, tripPattern.getAlightingPossible())
        )
      );
    }

    return combinedList;
  }

  List<TripPatternForDates> createTripPatterns(
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    TransitDataProviderFilter filter
  ) {
    List<TripPatternForDate> tripPatternForDates = getTripPatternsForDateRange(
      additionalPastSearchDays,
      additionalFutureSearchDays,
      filter
    );

    return merge(transitSearchTimeZero, tripPatternForDates, filter);
  }

  private static List<TripPatternForDate> filterActiveTripPatterns(
    TransitLayer transitLayer,
    LocalDate date,
    boolean firstDay,
    TransitDataProviderFilter filter
  ) {
    // On the first search day we want to add both TripPatternsForDate objects that start that day
    // and any previous day, while on subsequent search days we only want to add the
    // TripPatternForDate objects that start on that particular day. This is to prevent duplicates.

    return transitLayer
      .getTripPatternsForDate(date)
      .stream()
      .filter(filter::tripPatternPredicate)
      .filter(p -> firstDay || p.getStartOfRunningPeriod().toLocalDate().equals(date))
      .map(p -> p.newWithFilteredTripTimes(filter::tripTimesPredicate))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private List<TripPatternForDate> getTripPatternsForDateRange(
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    TransitDataProviderFilter filter
  ) {
    List<TripPatternForDate> tripPatternForDates = new ArrayList<>();

    // This filters trips by the search date as well as additional dates before and after
    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      tripPatternForDates.addAll(
        filterActiveTripPatterns(transitLayer, departureDate.plusDays(d), d == 0, filter)
      );
    }
    return tripPatternForDates;
  }
}
