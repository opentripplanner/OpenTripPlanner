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
    private final DebugHandler<ArrivalView<T>> debugHandlerStopArrivals;

    StateDebugger(
            StopsCursor<T> cursor,
            RoundProvider roundProvider,
            DebugHandlerFactory<T> dFactory
    ) {
        this.cursor = cursor;
        this.roundProvider = roundProvider;
        this.debugHandlerStopArrivals = dFactory.debugStopArrival();
    }

    void acceptAccess(int stop) {
        if(isDebug(stop)) {
            accept(stop);
        }
    }

    void dropOldStateAndAcceptNewState(int stop, Runnable body) {
        if (isDebug(stop)) {
            drop(stop);
            body.run();
            accept(stop);
        } else {
            body.run();
        }
    }

    void rejectTransit(int alightStop, int alightTime, T trip, int boardStop, int boardTime) {
        if (isDebug(alightStop)) {
            reject(cursor.rejectedTransit(round(), alightStop, alightTime, trip, boardStop, boardTime));
        }
    }

    void rejectTransfer(int fromStop, RaptorTransfer transferLeg, int toStop, int arrivalTime) {
        if (isDebug(transferLeg.stop())) {
            reject(cursor.rejectedTransfer(round(), fromStop, transferLeg, toStop, arrivalTime));
        }
    }


    /* Private methods */

    private boolean isDebug(int stop) {
        return debugHandlerStopArrivals.isDebug(stop);
    }

    private void accept(int stop) {
        debugHandlerStopArrivals.accept(cursor.stop(round(), stop));
    }

    private void drop(int stop) {
        if(cursor.exist(round(), stop)) {
            debugHandlerStopArrivals.drop(cursor.stop(round(), stop), null, null);
        }
    }

    private void reject(ArrivalView<T> arrival) {
        debugHandlerStopArrivals.reject(arrival, null, null);
    }

    private int round() {
        return roundProvider.round();
    }

}
