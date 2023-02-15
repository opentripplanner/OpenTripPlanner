package org.opentripplanner.transit.model.trip.timetable;

import org.opentripplanner.transit.model.trip.Timetable;

class BoardTripIndexSearch {

  /**
   * The BoardTimeSearchTest#compareSearchPerformance test is used find the optimal
   * threshold. The weakness in the test is that it uses a random normal distribution for
   * generating trip times.
   */
  static final int THRESHOLD_LINEAR_APPROX_MIN_LIMIT = 10;

  /**
   * Threshold for when we switch from binary search to linear search. This depends on the
   * architecture, but our experience is that a value between 10 and 20 performs well.
   */
  static final int BINARY_SEARCH_THRESHOLD = 10;

  /**
   * A binary search is better than the linear approximation for very large
   * and none uniform timetables; Hence we switch a binary search is if the number of
   * trips are above this threshold. To determine the best threshold value is a difficult
   * task and highly depend on the data, so we have used the
   * {@code DepartureSearchTest#compareSearchPerformance} test to make a qualified guess.
   * The binary search is safe, it performs well in skew distributions while the linear
   * approximation perform well as long as the trips are evenly spreed out in time. Hence,
   * we set this to low conservative value, even if the linear approximation is better for
   * high frequency timetables, up to 1000 trips per table.
   */
  static final int LINEAR_APPROXIMATION_TO_BINARY_THRESHOLD = 250;

  static TimetableTripIndexSearch createSearch(int nTrips) {
    // The DepartureSearchTest#compareSearchPerformance is used to determine when to use the
    // different searches
    if (nTrips <= THRESHOLD_LINEAR_APPROX_MIN_LIMIT) {
      return BoardTripIndexSearch::findBoardTime;
    } else if (nTrips <= LINEAR_APPROXIMATION_TO_BINARY_THRESHOLD) {
      return BoardTripIndexSearch::findBoardTimeLinearApproximation;
    } else {
      return BoardTripIndexSearch::findBoardTimeBinarySearch;
    }
  }

  static int search(final int[] a, final int start, final int end, final int edt) {
    if (edt < a[start]) {
      return Timetable.PREV_TIME_TABLE_INDEX;
    }
    // The DepartureSearchTest#compareSearchPerformance is used to determine when to use the
    // different searches
    int nTrips = end - start;
    if (nTrips <= THRESHOLD_LINEAR_APPROX_MIN_LIMIT) {
      return findBoardTime(a, start, end, edt);
    } else if (nTrips <= LINEAR_APPROXIMATION_TO_BINARY_THRESHOLD) {
      return findBoardTimeLinearApproximation(a, start, end, edt);
    } else {
      return findBoardTimeBinarySearch(a, start, end, edt);
    }
  }

  /** Implement {@link TimetableTripIndexSearch#searchForTripIndex(int[], int, int, int)} */
  static int findBoardTime(final int[] a, final int start, final int end, final int edt) {
    if (edt > a[end - 1]) {
      return Timetable.NEXT_TIME_TABLE_INDEX;
    }
    return findBoardTimeBasic(a, start, end, edt);
  }

  /** Implement {@link TimetableTripIndexSearch#searchForTripIndex(int[], int, int, int)} */
  static int findBoardTimeLinearApproximation(
    final int[] a,
    final int start,
    final int end,
    final int edt
  ) {
    if (edt < a[start]) {
      return Timetable.PREV_TIME_TABLE_INDEX;
    }
    if (edt > a[end - 1]) {
      return Timetable.NEXT_TIME_TABLE_INDEX;
    }

    // Before we interpolate to find the index of the expected trip, we check each end of the
    // timetable. This fixes two problems, let N be THRESHOLD_LINEAR_APPROX_MIN_LIMIT:
    //  - we can safly jump at least N elements in both ends after the interpolation without risking
    //    going outside of the timetable.
    //  - by removing N in the beginning and end of the timetable we remove make the dwell times/
    //    frequency more stable, since a very comon pattern is to rampup/down the frequency of a
    //    trip pattern in the beginning and end of a day

    // Skip linear approximation if one of first N elements
    int s = start + THRESHOLD_LINEAR_APPROX_MIN_LIMIT;
    if (edt < a[s]) {
      return findBoardTimeBasic(a, start, end, edt);
    }

    // Skip linear approximation if one of last N elements
    int e = (end - 1) - THRESHOLD_LINEAR_APPROX_MIN_LIMIT;
    if (edt > a[e]) {
      return findBoardTimeBasic(a, e, end, edt);
    }

    // Guess index based on linear approximation
    int i =
      TimetableIntUtils.interpolateFloor(a[s], s, a[e], e, edt) -
      THRESHOLD_LINEAR_APPROX_MIN_LIMIT /
      2;

    // Move to a index which is guaranteed to have a time less than edt
    while (edt < a[i]) {
      i -= THRESHOLD_LINEAR_APPROX_MIN_LIMIT;
    }
    return findBoardTimeBasic(a, i, end, edt);
  }

  /**
   * Do a binary search to find the approximate upper bound index for where to start the search.
   * <p/>
   * This is just a guess, and we return when the trip with a best valid departure is in the range
   * of the next {@link #BINARY_SEARCH_THRESHOLD}.
   *
   * @return a better upper bound index (exclusive)
   */
  static int findBoardTimeBinarySearch(
    final int[] a,
    final int start,
    final int end,
    final int edt
  ) {
    if (edt < a[start]) {
      return Timetable.PREV_TIME_TABLE_INDEX;
    }

    int lower = start;
    int upper = end;

    while (upper - lower > BINARY_SEARCH_THRESHOLD) {
      int m = (lower + upper) / 2;

      if (a[m] > edt) {
        upper = m;
      } else {
        lower = m;
      }
    }

    return findBoardTimeBasic(a, lower, upper, edt);
  }

  private static int findBoardTimeBasic(
    final int[] a,
    final int start,
    final int end,
    final int edt
  ) {
    for (int i = start; i < end; ++i) {
      if (edt <= a[i]) {
        return i % (end - start);
      }
    }
    return Timetable.NEXT_TIME_TABLE_INDEX;
  }
}
