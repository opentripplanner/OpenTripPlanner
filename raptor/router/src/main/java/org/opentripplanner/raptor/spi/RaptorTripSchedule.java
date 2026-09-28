package org.opentripplanner.raptor.spi;

import java.util.BitSet;

/**
 * The purpose of this interface is to provide information about the trip schedule. The trip is a
 * child of, and lives in the context of a trip pattern.
 * <p/>
 * The purpose of hiding these attributes behind an interface is to allow the implementation to
 * choose the most efficient underlying representation that suits its needs.
 */
public interface RaptorTripSchedule {
  /**
   * An id/index for the trip which can be used to sort trips so they follow each other in time. The
   * id/index must increase with the departure time.
   */
  int tripSortIndex();

  /**
   * The arrival time at the given stop position in pattern.
   *
   * @param stopPosInPattern the stop position.
   * @return the arrival time in seconds at the given stop
   */
  int arrival(int stopPosInPattern);

  /**
   * The departure time at the given stop position in pattern.
   *
   * @param stopPosInPattern the stop position.
   * @return the departure time in seconds at the given stop
   */
  int departure(int stopPosInPattern);

  /// The relative-travel-duration is a proxy for time spent on transit from the boarding stop to
  /// the alight stop. We do not know the alight stop, so it is impossible to calculate the
  /// "correct" time. The only thing that matters is that the relative difference between two
  /// boardings is correct. Compute a relative-time that can be used to compare the travel-time
  /// cost for any two boardings in the same pattern.
  ///
  /// Two invariants must hold:
  ///
  /// - For two boardings at stop positions `i` and `j` on the same trip (where `i` comes before
  ///   `j` in the pattern), `relativeTravelDuration(boardAtI) - relativeTravelDuration(boardAtJ)`
  ///   must equal the actual transit duration from stop `i` to stop `j`. If the board time at
  ///   position 3 is 10:00 and at the next stop is 10:05, then the value at position 3 is larger
  ///   by 5*60s = 300 than at the next stop.
  /// - All trips in the same pattern should return the same value for at least one stop. If you
  ///   choose to anchor the relative duration to the last stop, and the value for one trip at the
  ///   last stop is 56_000, then it must be 56_000 at the last stop for all other trips in the
  ///   same pattern.
  int relativeTravelDuration(int boardTime);

  /**
   * Return the pattern for this trip.
   */
  RaptorTripPattern pattern();

  /**
   * Search for the arrival stop position for the latest arrival time and stop index. We need the
   * time in addition to the stop in cases where the trip pattern loops, visits the same stop
   * twice. Also, the time alone is not enough, since more than one stop could have the exact
   * same arrival time.
   * <p>
   * Consider using position-based methods in {@link RaptorTripPattern} if possible.
   *
   * @return the stop position in the trip pattern if found; otherwise, -1
   */
  default int findArrivalStopPosition(int latestArrivalTime, int stop) {
    var p = pattern();
    int i = p.findAlightStopPositionBefore(p.numberOfStopsInPattern(), stop);

    while (i != -1 && arrival(i) > latestArrivalTime) {
      i = p.findAlightStopPositionBefore(i, stop);
    }
    return i;
  }

  /**
   * Same as {@code #findDepartureStopPosition(0, earliestDepartureTime, stop)}.
   * @see #findDepartureStopPosition(int, int, int)
   */
  default int findDepartureStopPosition(int earliestDepartureTime, int stop) {
    return findDepartureStopPosition(0, earliestDepartureTime, stop);
  }

  /**
   * Find the departure stop position for a stop index after the given earliest departure time
   * (inclusive), starting the search at the given start stop position. This method returns the
   * first stop position found, or -1 if no stop position is found.
   * <p>
   * Consider using position-based methods in {@link RaptorTripPattern} if possible.
   *
   * @param startStopPos the start stop position to search from
   * @param earliestDepartureTime the earliest departure time to search for (inclusive)
   * @param stop the stop index to search for
   * @return the stop position in the trip pattern if found; otherwise, -1
   */
  default int findDepartureStopPosition(int startStopPos, int earliestDepartureTime, int stop) {
    var p = pattern();
    int i = p.findBoardStopPositionAfter(startStopPos, stop);

    while (i != -1 && departure(i) < earliestDepartureTime) {
      i = p.findBoardStopPositionAfter(i + 1, stop);
    }
    return i;
  }

  /**
   * Find all departure stop positions for a stop index after the given earliest departure time.
   * This is useful because a trip can pass through the same stop more than once if the stop pattern
   * is circular. This method returns all stop positions, while
   * {@link #findDepartureStopPosition} returns only the first stop position found.
   *
   * @return list of all valid stop positions for a given stop index
   */
  default IntIterator findDepartureStopPositions(int earliestDepartureTime, int stop) {
    // In the common case where there are zero or one stops, we can return early with an
    // IntIterators.empty() or IntIterators.singleValueIterator(), thereby avoiding the
    // overhead of creating a BitSet and adding a single stop to it.
    int i0 = findDepartureStopPosition(earliestDepartureTime, stop);
    if (i0 == -1) {
      return IntIterators.empty();
    }
    int i = findDepartureStopPosition(i0 + 1, earliestDepartureTime, stop);
    if (i == -1) {
      return IntIterators.singleValueIterator(i0);
    }
    var stops = new BitSet();
    stops.set(i0);
    do {
      stops.set(i);
      i = findDepartureStopPosition(i + 1, earliestDepartureTime, stop);
    } while (i != -1);
    return IntIterators.of(stops);
  }
}
