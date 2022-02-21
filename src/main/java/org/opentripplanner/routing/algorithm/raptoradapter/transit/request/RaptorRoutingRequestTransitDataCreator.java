package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static java.util.stream.Collectors.groupingBy;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper.secondsSinceStartOfTime;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;


/**
 * This class is responsible for creating the internal data structure of
 * {@link RaptorRoutingRequestTransitData}. The code is messy so it is nice to NOT have it in
 * the transit data class itself, to keep it clean. Another benefit of isolating this code
 * is that these methods are only available at construction time.
 */
class RaptorRoutingRequestTransitDataCreator {

  private final TransitLayer transitLayer;
  private final ZonedDateTime transitSearchTimeZero;
  private final LocalDate departureDate;


  RaptorRoutingRequestTransitDataCreator(TransitLayer transitLayer, ZonedDateTime transitSearchTimeZero) {
    this.transitLayer = transitLayer;
    this.departureDate = transitSearchTimeZero.toLocalDate();
    this.transitSearchTimeZero = transitSearchTimeZero;
  }

  List<List<TripPatternForDates>> createTripPatternsPerStop(
      int additionalPastSearchDays,
      int additionalFutureSearchDays,
      TransitDataProviderFilter filter
  ) {

    List<TripPatternForDate> tripPatternForDates = getTripPatternsForDateRange(
        additionalPastSearchDays,
        additionalFutureSearchDays,
        filter
    );

    List<TripPatternForDates> tripPatternForDateList = merge(transitSearchTimeZero, tripPatternForDates);

    return createTripPatternsPerStop(tripPatternForDateList, transitLayer.getStopCount());
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
        filterActiveTripPatterns(
          transitLayer,
          departureDate.plusDays(d),
          d == 0,
          filter
        )
      );
    }
    return tripPatternForDates;
  }

  /**
   * This method merges several list of TripPatterns for several consecutive dates into a single
   * list of TripPatternsForDates. The purpose of doing this is so that TripSchedules for several
   * dates are combined by TripPattern instead of having their own TripPattern. This is to improve
   * performance for searching, as each TripPattern is searched only once per round.
   */
  static List<TripPatternForDates> merge(
      ZonedDateTime transitSearchTimeZero, List<TripPatternForDate> patternForDateList
  ) {

    // Group TripPatternForDate objects by TripPattern
    Map<TripPatternWithRaptorStopIndexes, List<TripPatternForDate>> patternForDateByPattern = patternForDateList
        .stream()
        .collect(groupingBy(TripPatternForDate::getTripPattern));

    List<TripPatternForDates> combinedList = new ArrayList<>();

    // For each TripPattern, time expand each TripPatternForDate and merge into a single
    // TripPatternForDates
    for (Map.Entry<TripPatternWithRaptorStopIndexes, List<TripPatternForDate>> patternEntry : patternForDateByPattern
        .entrySet()) {

      // Sort by date
      List<TripPatternForDate> patternsSorted = patternEntry
          .getValue()
          .stream()
          .sorted(Comparator.comparing(TripPatternForDate::getLocalDate))
          .collect(Collectors.toUnmodifiableList());

      // Calculate offsets per date
      List<Integer> offsets = new ArrayList<>();
      for (TripPatternForDate tripPatternForDate : patternsSorted) {
        offsets.add(secondsSinceStartOfTime(transitSearchTimeZero, tripPatternForDate.getLocalDate()));
      }

      // Combine TripPatternForDate objects
      combinedList.add(new TripPatternForDates(patternEntry.getKey(), patternsSorted, offsets));
    }

    return combinedList;
  }

  private static List<List<TripPatternForDates>> createTripPatternsPerStop(
      List<TripPatternForDates> tripPatternsForDate, int numberOfStops
  ) {
    List<List<TripPatternForDates>> result = Stream
        .generate(ArrayList<TripPatternForDates>::new)
        .limit(numberOfStops)
        .collect(Collectors.toList());

    for (TripPatternForDates tripPatternForDateList : tripPatternsForDate) {
      for (int i : tripPatternForDateList.getTripPattern().getStopIndexes()) {
        result.get(i).add(tripPatternForDateList);
      }
    }
    return result;
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
}
