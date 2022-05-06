package org.opentripplanner.transit.model.trip.timetable;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.trip.Timetable;

public class DefaultTimetable implements Timetable {

  private final int hashCode;

  private final int nTrips;
  private final int nStops;
  private final int[] boardTimes;
  private final int[] alightTimes;
  private final int maxTripDurationInDays;
  private final TimeSearch boardSearch;
  private final TimeSearch alightSearch;

  DefaultTimetable(int nTrips, int nStops, int[] boardTimes, int[] alightTimes) {
    if (nStops * nTrips != alightTimes.length) {
      throw new IllegalArgumentException();
    }
    this.nTrips = nTrips;
    this.nStops = nStops;
    this.boardTimes = boardTimes;
    this.alightTimes = alightTimes;
    this.boardSearch = BoardTimeSearch.createSearch(numOfTrips());
    this.alightSearch = AlightTimeSearch.createSearch(numOfTrips());
    this.hashCode = TimetableIntUtils.matrixHashCode(nTrips, nStops, boardTimes, alightTimes);
    this.maxTripDurationInDays =
      calculateMaxTripDurationInDays(alightTimes, index(0, nStops - 1), alightTimes.length);
  }

  public static DefaultTimetable create(
    int[][] boardTimes,
    int[][] alightTimes,
    Deduplicator deduplicator
  ) {
    int[] bTimes = deduplicator.deduplicateIntArray(
      TimetableIntUtils.collapseAndFlipMatrix(boardTimes)
    );
    int[] aTimes = deduplicator.deduplicateIntArray(
      TimetableIntUtils.collapseAndFlipMatrix(alightTimes)
    );

    return new DefaultTimetable(boardTimes.length, boardTimes[0].length, bTimes, aTimes);
  }

  public static DefaultTimetable create(int[][] times, Deduplicator deduplicator) {
    int[] array = deduplicator.deduplicateIntArray(TimetableIntUtils.collapseAndFlipMatrix(times));
    return new DefaultTimetable(times.length, times[0].length, array, array);
  }

  @Override
  public final int numOfStops() {
    return nStops;
  }

  @Override
  public final int numOfTrips() {
    return nTrips;
  }

  @Override
  public int maxTripDurationInDays() {
    return 0;
  }

  @Override
  public final int boardTime(int tripIndex, int stopPos) {
    return boardTimes[index(tripIndex, stopPos)];
  }

  @Override
  public final int alightTime(int tripIndex, int stopPos) {
    return alightTimes[index(tripIndex, stopPos)];
  }

  @Override
  public int findTripIndexBoardingAfter(int stopPos, int earliestDepartureTime) {
    return findTripIndex(boardSearch, boardTimes, stopPos, earliestDepartureTime);
  }

  @Override
  public int findTripIndexAlightingBefore(int stopPos, int latestAlightTime) {
    return findTripIndex(alightSearch, alightTimes, stopPos, latestAlightTime);
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof DefaultTimetable other)) {
      return false;
    }
    if (hashCode != other.hashCode) {
      return false;
    }
    if (nTrips != other.nTrips) {
      return false;
    }
    if (nStops != other.nStops) {
      return false;
    }
    return (
      Arrays.equals(boardTimes, other.boardTimes) && Arrays.equals(alightTimes, other.alightTimes)
    );
  }

  private int findTripIndex(TimeSearch search, int[] times, int stopPos, int edt) {
    int start = offset(stopPos);
    return search.search(times, start, start + nTrips, edt);
  }

  private int index(int tripIndex, int stopPos) {
    return nStops * stopPos + tripIndex;
  }

  private int offset(int stopPos) {
    return nStops * stopPos;
  }

  private static int calculateMaxTripDurationInDays(int[] times, int start, int end) {
    int maxSec = Arrays.stream(times, start, end).max().getAsInt();
    return maxSec / (int) ChronoUnit.DAYS.getDuration().toSeconds();
  }
}
