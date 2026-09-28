package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * This class is used to find the board and alight time for a known trip, where you now the
 * board-stop and the alight-stop. You must also specify either an earliest-board-time or a
 * latest-alight-time - this is done to avoid boarding at the correct stop, but at the wrong time.
 * This can happen, for patterns go in a loop, visit the same stop more than once.
 * <p>
 * This class is used to find board- and alight-times for transfer paths when mapping stop-arrivals
 * to paths. The board and alight times are not stored in the stop-arrival state to save memory and
 * to speed up the search. Searching for this after the search is done to create paths is ok, since
 * the number of paths is a very small number compared to stop-arrivals during the search.
 */
public final class TripTimesSearch<T extends RaptorTripSchedule> {

  private TripTimesSearch() {}

  /**
   * Search for board- and alight-times for the trip matching the given stop-arrival when searching
   * FORWARD. Hence, searching in the same direction as the trip travel direction.
   */
  public static <S extends RaptorTripSchedule> BoardAndAlightTime findTripForwardSearch(
    ArrivalView<S> arrival
  ) {
    var transit = arrival.transitPath();
    var trip = transit.trip();
    int boardStopPos = transit.boardStopPosition();
    int alightStopPosition = trip
      .pattern()
      .findAlightStopPositionAfter(boardStopPos, arrival.stop());

    if (alightStopPosition == -1) {
      throw new IllegalStateException(
        "Alight stop position not found: " +
          " boardStopPosition: " +
          boardStopPos +
          ", arrival.stop: " +
          arrival.stop() +
          ", pattern: " +
          trip.pattern().debugInfo() +
          "]"
      );
    }

    return new BoardAndAlightTime(trip, boardStopPos, alightStopPosition);
  }

  /**
   * Search for board- and alight-times for the trip matching the given stop-arrival when searching
   * in REVERSE. Hence, searching in the opposite direction of the trip travel direction.
   */
  public static <S extends RaptorTripSchedule> BoardAndAlightTime findTripReverseSearch(
    ArrivalView<S> arrival
  ) {
    var transit = arrival.transitPath();
    var trip = transit.trip();
    int alightStopPosition = transit.boardStopPosition();
    int boardStopPos = trip
      .pattern()
      .findBoardStopPositionBefore(alightStopPosition, arrival.stop());

    if (boardStopPos == -1) {
      throw new IllegalStateException(
        "Board stop position not found: " +
          " alightStopPosition: " +
          alightStopPosition +
          ", arrival.stop: " +
          arrival.stop() +
          ", pattern: " +
          trip.pattern().debugInfo() +
          "]"
      );
    }

    return new BoardAndAlightTime(trip, boardStopPos, alightStopPosition);
  }
}
