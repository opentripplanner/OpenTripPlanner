package org.opentripplanner.transit.model.trip.timetable;

import org.opentripplanner.transit.model.trip.Timetable;

class AlightTimeSearch {

  static TimeSearch createSearch(int nTrips) {
    // The DepartureSearchTest#compareSearchPerformance is used to determine when to use the
    // different searches
    if (nTrips <= BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT) {
      return AlightTimeSearch::findAlightTime;
    }
    if (nTrips <= 500) {
      return AlightTimeSearch::findAlightTimeLinearApproximation;
    }
    return AlightTimeSearch::findAlightTimeBinarySearch;
  }

  /** Implement {@link TimeSearch#search(int[], int, int, int)} */
  static int findAlightTime(final int[] a, final int start, final int end, final int lat) {
    if (lat > a[end]) {
      return Timetable.NEXT_TIME_TABLE_INDEX;
    }
    return findAlightTimeBasic(a, start, end, lat);
  }

  /** Implement {@link TimeSearch#search(int[], int, int, int)} */
  static int findAlightTimeLinearApproximation(
    final int[] a,
    final int start,
    final int end,
    final int lat
  ) {
    if (lat > a[end]) {
      return Timetable.NEXT_TIME_TABLE_INDEX;
    }
    if (lat < a[start + 1]) {
      return Timetable.PREV_TIME_TABLE_INDEX;
    }

    // Before we interpolate to find the index of the expected trip, we check each end of the
    // timetable. This fixes two problems, let N be THRESHOLD_LINEAR_APPROX_MIN_LIMIT:
    //  - we can safly jump at least N elements in both ends after the interpolation without risking
    //    going outside of the timetable.
    //  - by removing N in the beginning and end of the timetable we remove make the dwell times/
    //    frequency more stable, since a very comon pattern is to rampup/down the frequency of a
    //    trip pattern in the beginning and end of a day

    // Skip linear approximation if one of last N elements
    int e = end - BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT;
    if (lat > a[e]) {
      return findAlightTimeBasic(a, start, end, lat);
    }

    // Skip linear approximation if one of first N elements
    int s = (start + 1) + BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT;
    if (lat < a[s]) {
      return findAlightTimeBasic(a, start, s, lat);
    }

    // Guess index based on linear approximation
    int i =
      TimetableIntUtils.interpolateCeiling(a[s], s, a[e], e, lat) +
      BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT /
      2;

    // Move to a index which is guaranteed to have a time less than lat
    while (lat > a[i]) {
      i += BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT;
    }
    return findAlightTimeBasic(a, start, i, lat);
  }

  /**
   * Do a binary search to find the approximate upper bound index for where to start the search.
   * <p/>
   * This is just a guess, and we return when the trip with a best valid departure is in the range
   * of the next {@link BoardTimeSearch#BINARY_SEARCH_THRESHOLD}.
   *
   * @return a better upper bound index (exclusive)
   */
  static int findAlightTimeBinarySearch(
    final int[] a,
    final int start,
    final int end,
    final int lat
  ) {
    if (lat > a[end]) {
      return Timetable.NEXT_TIME_TABLE_INDEX;
    }
    int lower = start;
    int upper = end;

    while (upper - lower > BoardTimeSearch.BINARY_SEARCH_THRESHOLD) {
      int m = (lower + upper) / 2;

      if (a[m] < lat) {
        lower = m;
      } else {
        upper = m;
      }
    }

    return findAlightTimeBasic(a, lower, upper, lat);
  }

  private static int findAlightTimeBasic(
    final int[] a,
    final int start,
    final int end,
    final int lat
  ) {
    for (int i = end; i > start; --i) {
      if (lat >= a[i]) {
        return i;
      }
    }
    return Timetable.PREV_TIME_TABLE_INDEX;
  }
}
