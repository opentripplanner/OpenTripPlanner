package org.opentripplanner.transit.raptor.rangeraptor.standard.debug;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;

import java.util.Collection;


/**
 * The responsibility of this class is to wrap a {@link StopArrivalsState} and notify the
 * {@link org.opentripplanner.transit.raptor.rangeraptor.standard.debug.StateDebugger} about all stop arrival events.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class DebugStopArrivalsState<T extends RaptorTripSchedule> implements StopArrivalsState<T> {

    private final StopArrivalsState<T> delegate;
    private final StateDebugger<T> debug;

    /**
     * Create a Standard range raptor state for the given context
     */
    public DebugStopArrivalsState(
            RoundProvider roundProvider,
            DebugHandlerFactory<T> dFactory,
            StopsCursor<T> stopsCursor,
            StopArrivalsState<T> delegate
    ) {
        this.debug = new StateDebugger<>(stopsCursor, roundProvider, dFactory);
        this.delegate = delegate;
    }

    @Override
    public final void setAccess(final int stop, final int arrivalTime, RaptorTransfer access) {
        delegate.setAccess(stop, arrivalTime, access);
        debug.acceptAccess(stop);
    }

    @Override
    public final Collection<Path<T>> extractPaths() {
        return delegate.extractPaths();
    }

    @Override
    public final int bestTimePreviousRound(int stop) {
        return delegate.bestTimePreviousRound(stop);
    }

    @Override
    public void setNewBestTransitTime(int stop, int alightTime, T trip, int boardStop, int boardTime, boolean newBestOverall) {
        debug.dropOldStateAndAcceptNewState(
                stop,
                () -> delegate.setNewBestTransitTime(stop, alightTime, trip, boardStop, boardTime, newBestOverall)
        );
    }

    @Override
    public void rejectNewBestTransitTime(int stop, int alightTime, T trip, int boardStop, int boardTime) {
        debug.rejectTransit(stop, alightTime, trip, boardStop, boardTime);
        delegate.rejectNewBestTransitTime(stop, alightTime, trip, boardStop, boardTime);
    }

    @Override
    public void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transferLeg) {
        debug.dropOldStateAndAcceptNewState(
                transferLeg.stop(),
                () -> delegate.setNewBestTransferTime(fromStop, arrivalTime, transferLeg)
        );
    }

    @Override
    public void rejectNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transferLeg) {
        debug.rejectTransfer(fromStop, transferLeg, transferLeg.stop(), arrivalTime);
        delegate.rejectNewBestTransferTime(fromStop, arrivalTime, transferLeg);
    }
}