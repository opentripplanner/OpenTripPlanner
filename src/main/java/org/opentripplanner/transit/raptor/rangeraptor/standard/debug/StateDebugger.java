package org.opentripplanner.transit.raptor.rangeraptor.standard.debug;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;

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

    void acceptAccessPath(int stop, boolean stopReachedOnBoard) {
        if(isDebug(stop)) {
            accept(stop, stopReachedOnBoard);
        }
    }

    void rejectAccessPath(RaptorTransfer accessPath, int arrivalTime) {
        if (isDebug(accessPath.stop())) {
            reject(cursor.fictiveAccess(round(), accessPath, arrivalTime));
        }
    }

    void dropOldStateAndAcceptNewOnBoardArrival(int stop, boolean newBestOverall, Runnable body) {
        if (isDebug(stop)) {
            drop(stop, newBestOverall);
            body.run();
            accept(stop, true);
        } else {
            body.run();
        }
    }

    void dropOldStateAndAcceptNewOnStreetArrival(int stop, Runnable body) {
        if (isDebug(stop)) {
            drop(stop, true);
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

    private void drop(int stop, boolean newBestOverall) {
        if(!cursor.exist(round(), stop)) { return; }

        ArrivalView<T> arrival = null;

        if(newBestOverall) {
            arrival = cursor.bestStopArrival(round(), stop);
        }
        else if (cursor.transitExist(round(), stop)) {
            arrival = cursor.transit(round(), stop);
        }

        if(arrival != null) {
            debugHandlerStopArrivals.drop(arrival, null, null);
        }
    }

    private void reject(ArrivalView<T> arrival) {
        debugHandlerStopArrivals.reject(arrival, null, null);
    }

    private int round() {
        return roundProvider.round();
    }

}
