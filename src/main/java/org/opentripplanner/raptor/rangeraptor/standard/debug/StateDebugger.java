package org.opentripplanner.raptor.rangeraptor.standard.debug;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.DebugHandler;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Send debug events to the {@link DebugHandler} using the {@link StopsCursor}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class StateDebugger<T extends RaptorTripSchedule> {

  private final StopsCursor<T> cursor;
  private final RoundProvider roundProvider;
  private final DebugHandler<ArrivalView<?>> debugHandlerStopArrivals;

  StateDebugger(
    StopsCursor<T> cursor,
    RoundProvider roundProvider,
    DebugHandlerFactory<T> dFactory
  ) {
    this.cursor = cursor;
    this.roundProvider = roundProvider;
    this.debugHandlerStopArrivals = dFactory.debugStopArrival();
  }

  void acceptAccessPath(int stop, RaptorAccessEgress access) {
    if (isDebug(stop)) {
      debugHandlerStopArrivals.accept(cursor.access(round(), stop, access));
    }
  }

  void rejectAccessPath(RaptorAccessEgress accessPath, int arrivalTime) {
    if (isDebug(accessPath.stop())) {
      reject(cursor.fictiveAccess(round(), accessPath, arrivalTime));
    }
  }

  void dropOldStateAndAcceptNewOnBoardArrival(int stop, boolean newBestOverall, Runnable body) {
    if (isDebug(stop)) {
      drop(stop, true, newBestOverall);
      body.run();
      accept(stop, true);
    } else {
      body.run();
    }
  }

  void dropOldStateAndAcceptNewOnStreetArrival(int stop, Runnable body) {
    if (isDebug(stop)) {
      drop(stop, false, true);
      body.run();
      accept(stop, false);
    } else {
      body.run();
    }
  }

  void rejectTransit(int alightStop, int alightTime, T trip, int boardStop, int boardTime) {
    if (isDebug(alightStop)) {
      reject(cursor.fictiveTransit(round(), alightStop, alightTime, trip, boardStop, boardTime));
    }
  }

  void rejectTransfer(int fromStop, RaptorTransfer transfer, int toStop, int arrivalTime) {
    if (isDebug(transfer.stop())) {
      reject(cursor.fictiveTransfer(round(), fromStop, transfer, toStop, arrivalTime));
    }
  }

  /* Private methods */

  private boolean isDebug(int stop) {
    return debugHandlerStopArrivals.isDebug(stop);
  }

  private void accept(int stop, boolean stopReachedOnBoard) {
    debugHandlerStopArrivals.accept(cursor.stop(round(), stop, stopReachedOnBoard));
  }

  /**
   * This method mimic the logic in the worker/state. A better approach would be to do the logging
   * where the changes happen as in the multi-criteria version, but that would lead to a much more
   * complicated instrumentation. So, this method replicate the logic that is done in the worker and
   * state classes combined.
   * <p>
   * Note! Arrivals in the state is not dropped by this method, this class only notify the debug
   * handler about arrivals that are about to be dropped.
   */
  private void drop(int stop, boolean onBoard, boolean newBestOverall) {
    final int round = round();

    // if new arrival arrived on-board,
    if (onBoard) {
      // and an existing on-board arrival exist
      if (cursor.reachedOnBoard(round, stop)) {
        dropExistingArrival(round, stop, onBoard);
      }
      // and an existing best-over-all arrival exist
      if (newBestOverall) {
        if (cursor.reachedOnStreet(round, stop)) {
          dropExistingArrival(round, stop, false);
        }
      }
    }
    // drop existing best over all arrival, but not existing on-board
    else if (cursor.reachedOnStreet(round, stop)) {
      dropExistingArrival(round, stop, onBoard);
    }
  }

  private void reject(ArrivalView<T> arrival) {
    debugHandlerStopArrivals.reject(arrival, null, null);
  }

  private void dropExistingArrival(int round, int stop, boolean onBoard) {
    debugHandlerStopArrivals.drop(cursor.stop(round, stop, onBoard), null, null);
  }

  private int round() {
    return roundProvider.round();
  }
}
