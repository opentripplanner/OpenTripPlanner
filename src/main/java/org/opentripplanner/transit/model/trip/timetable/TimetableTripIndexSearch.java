package org.opentripplanner.transit.model.trip.timetable;

@FunctionalInterface
interface TimetableTripIndexSearch {
  /**
   * Search for the closest trip index in {@link org.opentripplanner.transit.model.trip.Timetable}.
   * <p>
   * For a boarding search we want to find the trip index for the trip departing after the
   * given {@code time}.
   * <p>
   * For an alighting search we want to find the trip index for the trip arriving before the
   * given {@code time}.
   *
   * @param a The time sequence to search, must be increasing times.
   * @param start The start index, inclusive(board search), exclusive(alight search)
   * @param end The end index, exclusive(board search), inclusive(alight search)
   * @param time The earliest-board-time or latest-arrival-time limit
   * @return the index for the best boarding or alighting to use.
   */
  int searchForTripIndex(final int[] a, final int start, final int end, final int time);
}
