package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StopArrivalsState;

import java.util.Collection;


/**
 * Tracks the state necessary to construct paths at the end of each iteration.
 * <p/>
 * This class find the pareto optimal paths with respect to: rounds, arrival time and total travel time.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdStopArrivalsState<T extends RaptorTripSchedule> implements StopArrivalsState<T> {

    private final Stops<T> stops;
    private final DestinationArrivalPaths<T> results;

    /**
     * Create a Standard Range Raptor state for the given stops and destination arrivals.
     */
    public StdStopArrivalsState(Stops<T> stops, DestinationArrivalPaths<T> paths) {
        this.stops = stops;
        this.results = paths;
    }

    @Override
    public final void setAccess(final int stop, final int arrivalTime, RaptorTransfer access) {
        stops.setAccess(stop, arrivalTime, access);
    }

    @Override
    public final Collection<Path<T>> extractPaths() {
        return results.listPaths();
    }

    @Override
    public final int bestTimePreviousRound(int stop) {
        return stops.bestTimePreviousRound(stop);
    }


    @Override
    public void setNewBestTransitTime(int stop, int alightTime, T trip, int boardStop, int boardTime, boolean newBestOverall) {
        stops.transitToStop(stop, alightTime, boardStop, boardTime, trip, newBestOverall);
    }

    @Override
    public void rejectNewBestTransitTime(int stop, int alightTime, T trip, int boardStop, int boardTime) {
    }

    @Override
    public void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transferLeg) {
        stops.transferToStop(fromStop, transferLeg, arrivalTime);
    }

    @Override
    public void rejectNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transferLeg) {
    }
}