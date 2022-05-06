package org.opentripplanner.transit.model.trip.timetable;

@FunctionalInterface
interface TimeSearch {
  /**
   * Search for best departure or arrival time given the time.
   * @param a The time sequence to search, must be increasing times.
   * @param start The start index, inclusive(board search), exclusive(alight search)
   * @param end The end index, exclusive(board search), inclusive(alight search)
   * @param time The earliest-board-time or latest-arrival-time limit
   * @return the index for the best boarding or alighting to use.
   */
  int search(final int[] a, final int start, final int end, final int time);
}
