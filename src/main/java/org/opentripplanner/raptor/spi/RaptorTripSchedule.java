package org.opentripplanner.raptor.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * The purpose of this interface is to provide information about the trip schedule. The trip is a
 * child of, and lives in the context of a trip pattern.
 * <p/>
 * The purpose of hiding these attributes behind an interface is to allow the implementation to
 * chose the most efficient underlying implementation that suits its needs.
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
   * Search for the arrival time for the given stopIndex. This is not optimized for performance.
   *
   * @param startStopPos the stop position in pattern to start search(inclusive).
   * @param stopIndex    the stopIndex to find the arrival time for.
   * @return the arrival time in seconds at the given stop
   * @throws IndexOutOfBoundsException if stopIndex not found
   */
  default int arrival(int startStopPos, int stopIndex) {
    return arrival(pattern().findStopPositionAfter(startStopPos, stopIndex));
  }

  /**
   * The departure time at the given stop position in pattern.
   *
   * @param stopPosInPattern the stop position.
   * @return the arrival time in seconds at the given stop
   */
  int departure(int stopPosInPattern);

  /**
   * Search for the departure time for the given stopIndex. This is not optimized for performance.
   *
   * @param startStopPos the stop position in pattern to start the search(inclusive).
   * @param stopIndex    the stopIndex to find the departure time for.
   * @return the arrival time in seconds at the given stop
   * @throws IndexOutOfBoundsException if stopIndex not found
   */
  default int departure(int startStopPos, int stopIndex) {
    return departure(pattern().findStopPositionAfter(startStopPos, stopIndex));
  }

  /**
   * Return the pattern for this trip.
   */
  RaptorTripPattern pattern();

  /**
   * Search for departure-stop-position for the given trip, earliest-departure-time and stop index.
   * We need the time in addition to the stop in cases were the trip pattern visit the same stop
   * twice. Also the time is not sufficient, since more than one stop could have the exact same
   * departure time.
   * <p>
   * Raptor save memory by NOT storing the board/arrival stop positions in pattern; Hence we need
   * this method when mapping into a itinerary or raptor path.
   * <p>
   * Avoid using this during routing, it is not optimized for performance.
   *
   * @return the stop-position in the trip pattern if found, if not -1 is returned.
   */
  default int findArrivalStopPosition(int latestArrivalTime, int stop) {
    RaptorTripPattern p = pattern();
    int i = p.numberOfStopsInPattern() - 1;

    while (arrival(i) > latestArrivalTime) {
      --i;
      if (i == -1) {
        return -1;
      }
    }
    return p.findStopPositionBefore(i, stop);
  }

  /**
   * Search for departure-stop-position for the given trip, earliest-departure-time and stop index.
   * We need the time in addition to the stop in cases were the trip pattern visit the same stop
   * twice. Also the time is not sufficient, since more than one stop could have the exact same
   * departure time.
   * <p>
   * Raptor save memory by NOT storing the board/arrival stop positions in pattern; Hence we need
   * this method when mapping into a itinerary or raptor path.
   * <p>
   * Avoid using this during routing, it is not optimized for performance.
   *
   * @return the stop-position in the trip pattern if found, if not -1 is returned.
   */
  default int findDepartureStopPosition(int earliestDepartureTime, int stop) {
    var p = pattern();
    final int size = p.numberOfStopsInPattern();
    int i = 0;

    while (departure(i) < earliestDepartureTime) {
      ++i;
      if (i == size) {
        return -1;
      }
    }
    return p.findStopPositionAfter(i, stop);
  }

  /**
   * Find all departure-stop-positions for a stop index after given earliest-departure-time. This is
   * useful because trip can pass through the same stop more than once - if the stop pattern is circular.
   * This method returns all stop positions, while the {@link #findDepartureStopPosition} only returns the first
   * stop-position found.
   *
   * @return list of all valid stop positions for a given stop index
   */
  default List<Integer> findDepartureStopPositions(int earliestDepartureTime, int stop) {
    var p = pattern();
    final int size = p.numberOfStopsInPattern();
    int i = 0;

    while (departure(i) < earliestDepartureTime) {
      ++i;
      if (i == size) {
        return new ArrayList<>();
      }
    }

    var stops = new ArrayList<Integer>();

    while (i < size) {
      if (stop == p.stopIndex(i)) {
        stops.add(i);
      }

      i++;
    }

    return stops;
  }
}
