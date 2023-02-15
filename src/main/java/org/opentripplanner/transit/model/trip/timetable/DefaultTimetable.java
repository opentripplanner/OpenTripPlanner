package org.opentripplanner.transit.model.trip.timetable;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.trip.Timetable;

/**
 * Implementation of {@link Timetable} witch store board and alight times in one
 * long array.
 * <p>
 * When Raptor searches for a boarding it searches all trips for a given stop, so it is faster
 * if the boarding/alighting for the same stop and different trips are close to each other in
 * memory; Hence the first `nTrip` entries in the array is the stop=0 and trip=0-(n-1).
 * <p>
 * TODO RTM - Develop other implementaion and use statistics to create the ones with
 *          - performs the best.
 */
public class DefaultTimetable implements Timetable {

  private final int hashCode;

  private final int nTrips;
  private final int nStops;
  private final int[] boardTimes;
  private final int[] alightTimes;
  private final int maxTripDurationInDays;
  private final TimetableTripIndexSearch boardSearch;
  private final TimetableTripIndexSearch alightSearch;

  DefaultTimetable(int nTrips, int nStops, int[] boardTimes, int[] alightTimes) {
    if (nStops * nTrips != alightTimes.length) {
      throw new IllegalArgumentException();
    }
    this.nTrips = nTrips;
    this.nStops = nStops;
    this.boardTimes = boardTimes;
    this.alightTimes = alightTimes;
    this.boardSearch = BoardTripIndexSearch.createSearch(nTrips);
    this.alightSearch = AlightTripIndexSearch.createSearch(nTrips);
    this.hashCode = TimetableIntUtils.matrixHashCode(nTrips, nStops, boardTimes, alightTimes);
    this.maxTripDurationInDays =
      calculateMaxTripDurationInDays(alightTimes, index(nTrips - 1, 0), alightTimes.length);
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
    return maxTripDurationInDays;
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

  private int findTripIndex(TimetableTripIndexSearch search, int[] times, int stopPos, int edt) {
    int start = offset(stopPos);
    return search.searchForTripIndex(times, start, start + nTrips, edt);
  }

  private int index(int tripIndex, int stopPos) {
    return offset(stopPos) + tripIndex;
  }

  private int offset(int stopPos) {
    return nTrips * stopPos;
  }

  @Override
  public String toString() {
    ToStringBuilder buf = ToStringBuilder
      .of(DefaultTimetable.class)
      .addNum("nTrips", nTrips)
      .addNum("nStops", nStops);
    if (maxTripDurationInDays > 0) {
      buf.addNum("maxTripDuration", maxTripDurationInDays, "d");
    }

    // The Deduplicator should ensure that these are the same if they are equal
    if (boardTimes == alightTimes) {
      buf.addObj("times", timesToString(boardTimes));
    } else {
      buf
        .addObj("boardTimes", timesToString(boardTimes))
        .addObj("alightTimes", timesToString(alightTimes));
    }
    return buf.toString();
  }

  /**
   * Sample trip-times, add first, middle and last trip to string.
   */
  private String timesToString(int[] times) {
    StringBuilder buf = new StringBuilder();
    buf.append("{");
    // Append the first trip in the timetable
    appendTripTimesToString(buf, times, 0);

    // Append the middle trip in the timetable
    if (nTrips > 2) {
      buf.append(", ");
      appendTripTimesToString(buf, times, (nTrips - 1) / 2);
    }
    // Append the last trip in the timetable
    if (nTrips > 1) {
      buf.append(", ");
      appendTripTimesToString(buf, times, nTrips - 1);
    }
    buf.append("}");
    return buf.toString();
  }

  private void appendTripTimesToString(StringBuilder buf, int[] times, int tripIndex) {
    buf.append("trip ").append(tripIndex).append(": ").append("[");
    for (int s = 0; s < nStops; s++) {
      if (s != 0) {
        buf.append(' ');
      }
      buf.append(TimeUtils.timeToStrCompact(times[index(tripIndex, s)]));
    }
    buf.append("]");
  }

  private static int calculateMaxTripDurationInDays(int[] times, int start, int end) {
    int maxSec = Arrays.stream(times, start, end).max().getAsInt();
    return maxSec / (int) ChronoUnit.DAYS.getDuration().toSeconds();
  }
}
