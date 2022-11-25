package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorTripPattern;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * This class is used to find the board and alight time for a known trip, where you now the
 * board-stop and the alight-stop. You must also specify either a earliest-board-time or a
 * latest-alight-time - this is done to avoid boarding at the correct stop, but at the wrong time.
 * This can happen for patterns goes in a loop, visit the same stop more than once.
 * <p>
 * This class is used to find board- and alight-times for transfer paths when mapping stop-arrivals
 * to paths. The board and alight times are not stored in the stop-arrival state to save memory and
 * to speed up the search. Searching for this after the search is done to create paths is ok, since
 * the number of paths are a very small number compared to stop-arrivals during the search.
 */
public class TripTimesSearch<T extends RaptorTripSchedule> {

  private final T schedule;
  private final int fromStop;
  private final int toStop;

  private TripTimesSearch(T schedule, int fromStop, int toStop) {
    this.schedule = schedule;
    this.fromStop = fromStop;
    this.toStop = toStop;
  }

  /**
   * Search for board- and alight-times for the trip matching the given stop-arrival when searching
   * FORWARD. Hence, searching in the same direction as the trip travel direction.
   */
  public static <S extends RaptorTripSchedule> BoardAndAlightTime findTripForwardSearch(
    ArrivalView<S> arrival
  ) {
    var transit = arrival.transitPath();
    var search = new TripTimesSearch<>(transit.trip(), transit.boardStop(), arrival.stop());
    return search.findTripTimesBefore(arrival.arrivalTime());
  }

  /**
   * Search for board- and alight-times for the trip matching the given stop-arrival when searching
   * in REVERSE. Hence, searching in the opposite direction of the trip travel direction.
   */
  public static <S extends RaptorTripSchedule> BoardAndAlightTime findTripReverseSearch(
    ArrivalView<S> arrival
  ) {
    var transit = arrival.transitPath();
    var search = new TripTimesSearch<>(transit.trip(), arrival.stop(), transit.boardStop());
    return search.findTripTimesAfter(arrival.arrivalTime());
  }

  /**
   * Search for board- and alight-times for the trip matching the given stop-arrival when searching
   * FORWARD. Hence, searching in the same direction as the trip travel direction.
   * <p>
   * This uses the approximate-time search, see {@link #findTripTimes(RaptorTripSchedule, int, int,
   * int)}.
   */
  public static <
    S extends RaptorTripSchedule
  > BoardAndAlightTime findTripForwardSearchApproximateTime(ArrivalView<S> arrival) {
    var t = arrival.transitPath();
    return findTripTimes(t.trip(), t.boardStop(), arrival.stop(), arrival.arrivalTime());
  }

  /**
   * Search for board- and alight-times for the trip matching the given stop-arrival when searching
   * in REVERSE. Hence, searching in the opposite direction of the trip travel direction.
   * <p>
   * This uses the approximate-time search, see {@link #findTripTimes(RaptorTripSchedule, int, int,
   * int)}.
   */
  public static <
    S extends RaptorTripSchedule
  > BoardAndAlightTime findTripReverseSearchApproximateTime(ArrivalView<S> arrival) {
    var t = arrival.transitPath();
    return findTripTimes(t.trip(), arrival.stop(), t.boardStop(), arrival.arrivalTime());
  }

  /**
   * Search for board- and alight-times for the trip matching the given {@code approximateTime}.
   * This is slower than the more specific methods and not as correct - it is possible to construct
   * cases with looping patterns where this method may find the wrong trip. But, it is safe - it
   * will always find a trip if it exists. Can be used in tests, logging and debugging, but avoid
   * using it in path mapping.
   */
  public static <S extends RaptorTripSchedule> BoardAndAlightTime findTripTimes(
    S trip,
    int fromStop,
    int toStop,
    int approximateTime
  ) {
    var search = new TripTimesSearch<>(trip, fromStop, toStop);
    return search.findTripByApproximateTime(approximateTime);
  }

  /* private methods */

  private BoardAndAlightTime findTripTimesAfter(final int earliestDepartureTime) {
    RaptorTripPattern p = schedule.pattern();
    final int size = p.numberOfStopsInPattern();

    int i = schedule.findDepartureStopPosition(earliestDepartureTime, fromStop);

    if (i < 0) {
      throw notFoundException(
        "No stops matching 'fromStop'",
        "earliestDepartureTime",
        earliestDepartureTime
      );
    }

    int boardStopPos = i;

    // Goto next stop, boarding and alighting can not happen on the same stop
    ++i;

    // Search for arrival
    while (i < size && p.stopIndex(i) != toStop) {
      ++i;
    }

    if (i == size) {
      throw notFoundException(
        "No stops matching 'toStop'",
        "earliestDepartureTime",
        earliestDepartureTime
      );
    }
    return new BoardAndAlightTime(schedule, boardStopPos, i);
  }

  private BoardAndAlightTime findTripTimesBefore(int latestArrivalTime) {
    RaptorTripPattern p = schedule.pattern();
    int i = schedule.findArrivalStopPosition(latestArrivalTime, toStop);

    if (i < 0) {
      throw notFoundException("No stops matching 'toStop'", "latestArrivalTime", latestArrivalTime);
    }

    int alightStopPos = i;

    // Goto next stop, boarding and alighting can not happen on the same stop
    --i;

    // Search for departure
    while (i >= 0 && p.stopIndex(i) != fromStop) {
      --i;
    }

    if (i < 0) {
      throw notFoundException(
        "No stops matching 'fromStop'",
        "latestArrivalTime",
        latestArrivalTime
      );
    }
    return new BoardAndAlightTime(schedule, i, alightStopPos);
  }

  /**
   * Find the trip that is closest in time to the given {@code approximateTime}.
   * <p>
   * If the approximate time is closer to the next trip, then the total time will be larger the 4
   * times the approximate time:
   * <ul>
   *   <li>Let the current trip board and alight times be: {@code a & b}</li>
   *   <li>Let the next trip board and alight times be: {@code c & d}</li>
   *   <li>Let approximate-time be: {@code T}</li>
   * </ul>
   * Then T is closer to [a,b] then [c,d] if:
   * <pre>
   *   x - (a+b)/2  < (c+d)/2 + x
   *   2x < (c+d)/2 + (a+b)/2
   *   4x <  (a+b+c+d)
   * </pre>
   */
  private BoardAndAlightTime findTripByApproximateTime(int approximateTime) {
    RaptorTripPattern p = schedule.pattern();

    int fromPos = stopPosAfter(p, 0, fromStop, approximateTime);
    int toPos = stopPosAfter(p, fromPos + 1, toStop, approximateTime);
    // Step back to find the closest departure stop in case there are more than one
    fromPos = stopPosBefore(p, toPos - 1, fromStop, approximateTime);

    try {
      while (true) {
        // Try to find next possible trip [if pattern runs in a loop]
        int nextFromPos = stopPosAfter(p, fromPos + 1, fromStop, approximateTime);
        int nextToPos = stopPosAfter(p, nextFromPos + 1, toStop, approximateTime);
        nextFromPos = stopPosBefore(p, nextToPos - 1, fromStop, approximateTime);

        int totTime =
          schedule.departure(fromPos) +
          schedule.arrival(toPos) +
          schedule.departure(nextFromPos) +
          schedule.arrival(nextToPos);

        if (totTime >= 4 * approximateTime) {
          break;
        }

        fromPos = nextFromPos;
        toPos = nextToPos;
      }
    } catch (IllegalStateException ignore) {
      /* No more times/loop exist */
    }

    return new BoardAndAlightTime(schedule, fromPos, toPos);
  }

  private int stopPosAfter(RaptorTripPattern p, int startPos, int stopIndex, int time) {
    int stopPos = p.findStopPositionAfter(startPos, stopIndex);
    return stopPosExist(stopPos, stopIndex, time);
  }

  private int stopPosBefore(RaptorTripPattern p, int startPos, int stopIndex, int time) {
    int stopPos = p.findStopPositionBefore(startPos, stopIndex);
    return stopPosExist(stopPos, stopIndex, time);
  }

  private int stopPosExist(int pos, int stop, int time) {
    if (pos < 0) {
      throw notFoundException("No stop matching " + stop, "approximateTime", time);
    }
    return pos;
  }

  private IllegalStateException notFoundException(String hint, String lbl, int time) {
    return new IllegalStateException(
      "Trip not found: " +
      hint +
      ". " +
      " [FromStop: " +
      fromStop +
      ", toStop: " +
      toStop +
      ", " +
      lbl +
      ": " +
      TimeUtils.timeToStrLong(time) +
      ", pattern: " +
      schedule.pattern().debugInfo() +
      "]"
    );
  }
}
