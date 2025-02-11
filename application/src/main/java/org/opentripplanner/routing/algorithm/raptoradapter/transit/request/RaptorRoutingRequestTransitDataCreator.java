package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.opentripplanner.utils.time.ServiceDateUtils.secondsSinceStartOfTime;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for creating the internal data structure of {@link
 * RaptorRoutingRequestTransitData}. The code is messy so it is nice to NOT have it in the transit
 * data class itself, to keep it clean. Another benefit of isolating this code is that these methods
 * are only available at construction time.
 */
class RaptorRoutingRequestTransitDataCreator {

  private static final Logger LOG = LoggerFactory.getLogger(
    RaptorRoutingRequestTransitDataCreator.class
  );

  private final RaptorTransitData raptorTransitData;
  private final ZonedDateTime transitSearchTimeZero;
  private final LocalDate departureDate;

  RaptorRoutingRequestTransitDataCreator(
    RaptorTransitData raptorTransitData,
    ZonedDateTime transitSearchTimeZero
  ) {
    this.raptorTransitData = raptorTransitData;
    this.departureDate = ServiceDateUtils.asServiceDay(transitSearchTimeZero);
    this.transitSearchTimeZero = transitSearchTimeZero;
  }

  public List<int[]> createTripPatternsPerStop(List<TripPatternForDates> tripPatternsForDate) {
    // Create temporary array of TIntArrayLists
    int stopCount = raptorTransitData.getStopCount();
    TIntArrayList[] patternsForStop = new TIntArrayList[stopCount];
    for (int i = 0; i < stopCount; i++) {
      patternsForStop[i] = new TIntArrayList();
    }

    // Loop through all patterns, and mark all stops containing that pattern
    for (TripPatternForDates tripPatternForDateList : tripPatternsForDate) {
      final RoutingTripPattern tripPattern = tripPatternForDateList.getTripPattern();
      final int patternIndex = tripPattern.patternIndex();
      final int numberOfStopsInPattern = tripPattern.numberOfStopsInPattern();
      for (int i = 0; i < numberOfStopsInPattern; i++) {
        patternsForStop[tripPattern.stopIndex(i)].add(patternIndex);
      }
    }

    // Create the final list with raw int arrays, in order to get the best iteration performance
    List<int[]> result = new ArrayList<>(stopCount);
    for (var patterns : patternsForStop) {
      result.add(patterns.toArray());
    }

    return result;
  }

  public List<TripPatternForDates> createPatternIndex(List<TripPatternForDates> tripPatterns) {
    TripPatternForDates[] result = new TripPatternForDates[RoutingTripPattern.indexCounter()];
    for (var pattern : tripPatterns) {
      result[pattern.getTripPattern().patternIndex()] = pattern;
    }
    return Arrays.asList(result);
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
    TransitDataProviderFilter filter,
    TransitGroupPriorityService transitGroupPriorityService
  ) {
    // Group TripPatternForDate objects by TripPattern.
    // This is done in a loop to increase performance.
    Map<RoutingTripPattern, List<TripPatternForDate>> patternForDateByPattern = new HashMap<>();
    for (TripPatternForDate patternForDate : patternForDateList) {
      patternForDateByPattern
        .computeIfAbsent(patternForDate.getTripPattern(), k -> new ArrayList<>())
        .add(patternForDate);
    }

    List<TripPatternForDates> combinedList = new ArrayList<>();

    TObjectIntMap<LocalDate> offsetCache = new TObjectIntHashMap<>();

    // For each TripPattern, time expand each TripPatternForDate and merge into a single
    // TripPatternForDates
    for (Map.Entry<RoutingTripPattern, List<TripPatternForDate>> patternEntry : patternForDateByPattern.entrySet()) {
      // Sort by date. We can mutate the array, as it was created above in the grouping.
      TripPatternForDate[] patternsSorted = patternEntry
        .getValue()
        .toArray(new TripPatternForDate[0]);
      Arrays.sort(patternsSorted);

      // Calculate offsets per date
      int[] offsets = new int[patternsSorted.length];
      for (int i = 0; i < patternsSorted.length; i++) {
        LocalDate serviceDate = patternsSorted[i].getServiceDate();
        if (offsetCache.containsKey(serviceDate)) {
          offsets[i] = offsetCache.get(serviceDate);
        } else {
          offsets[i] = secondsSinceStartOfTime(transitSearchTimeZero, serviceDate);
          offsetCache.put(serviceDate, offsets[i]);
        }
      }

      // Combine TripPatternForDate objects
      final RoutingTripPattern tripPattern = patternEntry.getKey();

      combinedList.add(
        new TripPatternForDates(
          tripPattern,
          patternsSorted,
          offsets,
          filter.filterAvailableStops(
            tripPattern,
            tripPattern.getBoardingPossible(),
            BoardAlight.BOARD
          ),
          filter.filterAvailableStops(
            tripPattern,
            tripPattern.getAlightingPossible(),
            BoardAlight.ALIGHT
          ),
          transitGroupPriorityService.lookupTransitGroupPriorityId(tripPattern.getPattern())
        )
      );
    }

    return combinedList;
  }

  List<TripPatternForDates> createTripPatterns(
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    TransitDataProviderFilter filter,
    TransitGroupPriorityService transitGroupPriorityService
  ) {
    List<TripPatternForDate> tripPatternForDates = getTripPatternsForDateRange(
      additionalPastSearchDays,
      additionalFutureSearchDays,
      filter
    );

    return merge(transitSearchTimeZero, tripPatternForDates, filter, transitGroupPriorityService);
  }

  private static List<TripPatternForDate> filterActiveTripPatterns(
    RaptorTransitData raptorTransitData,
    LocalDate date,
    boolean firstDay,
    TransitDataProviderFilter filter
  ) {
    // On the first search day we want to add both TripPatternsForDate objects that start that day
    // and any previous day, while on subsequent search days we only want to add the
    // TripPatternForDate objects that start on that particular day. This is to prevent duplicates.
    // This was previously a stream, but was unrolled for improved performance.

    Predicate<TripTimes> tripTimesWithSubmodesPredicate = tripTimes ->
      filter.tripTimesPredicate(tripTimes, filter.hasSubModeFilters());
    Predicate<TripTimes> tripTimesWithoutSubmodesPredicate = tripTimes ->
      filter.tripTimesPredicate(tripTimes, false);
    Collection<TripPatternForDate> tripPatternsForDate = raptorTransitData.getTripPatternsForRunningDate(
      date
    );
    List<TripPatternForDate> result = new ArrayList<>(tripPatternsForDate.size());
    for (TripPatternForDate p : tripPatternsForDate) {
      if (firstDay || p.getStartOfRunningPeriod().equals(date)) {
        if (filter.tripPatternPredicate(p)) {
          var tripTimesPredicate = p.getTripPattern().getPattern().getContainsMultipleModes()
            ? tripTimesWithSubmodesPredicate
            : tripTimesWithoutSubmodesPredicate;
          TripPatternForDate tripPatternForDate = p.newWithFilteredTripTimes(tripTimesPredicate);
          if (tripPatternForDate != null) {
            result.add(tripPatternForDate);
          }
        }
      }
    }
    return result;
  }

  private List<TripPatternForDate> getTripPatternsForDateRange(
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    TransitDataProviderFilter filter
  ) {
    List<TripPatternForDate> tripPatternForDates = new ArrayList<>();
    long start = System.currentTimeMillis();

    // This filters trips by the search date as well as additional dates before and after
    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      tripPatternForDates.addAll(
        filterActiveTripPatterns(raptorTransitData, departureDate.plusDays(d), d == 0, filter)
      );
    }

    if (LOG.isDebugEnabled()) {
      String time = DurationUtils.msToSecondsStr(System.currentTimeMillis() - start);
      long count = tripPatternForDates.size();
      LOG.debug("Prepare Transit model performed in {}, count: {}.", time, count);
    }

    return tripPatternForDates;
  }
}
