package org.opentripplanner.transit.model.trip;

/**
 * All bard and alight times for a given time period based on the board time
 * of the first boarding in the trip.
 */
public interface Timetable {
  int NOT_AVAILABLE = -900_000;
  int PREV_TIME_TABLE_INDEX = -901_000;
  int NEXT_TIME_TABLE_INDEX = -902_000;

  int numOfStops();

  int numOfTrips();

  /** Number of days the longest trip in this timetable run. This */
  int maxTripDurationInDays();

  /**
   * Search for the trip index boarding AFTER the given time at the given stop position.
   * Return {@link #NEXT_TIME_TABLE_INDEX} if time is after last time in table.
   */
  int findTripIndexBoardingAfter(int stopPos, int earliestDepartureTime);

  /**
   * Search for the trip index alighting BEFORE the given time at the given stop position.
   * Return {@link #PREV_TIME_TABLE_INDEX} if time is before first time in table.
   */
  int findTripIndexAlightingBefore(int stopPos, int latestAlightTime);

  /** Boarding time for a given trip index and stop position in pattern */
  int boardTime(int tripIndex, int stopPos);

  /** Alight time for a given trip index and stop position in pattern */
  int alightTime(int tripIndex, int stopPos);
}
