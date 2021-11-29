package org.opentripplanner.transit.raptor.rangeraptor.standard;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerState;

/**
 * This interface define the methods used be the {@link StdTransitWorker}
 * to query and update the state of the algorithm.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface StdWorkerState<T extends RaptorTripSchedule> extends WorkerState<T> {

    /**
     * Return TRUE if a stop is reached by transit or transfer in the previous round.
     */
    boolean isStopReachedInPreviousRound(int stop);

    /**
     * Return the best time at the given stop found in the last round. This is
     * used to find the right trip to board in the current round.
     * <p/>
     * If you are not trying to find paths or calculate the exact number of transfers
     * it is ok to return the overall best tim to reach the given stop.
     */
    int bestTimePreviousRound(int stop);

    /**
     * Set the time at a transit stop iff it is optimal. This sets both the bestTime and the transitTime
     */
    void transitToStop(int alightStop, int alightTime, int boardStop, int boardTime, T trip);

    TransitArrival<T> previousTransit(int boardStopIndex);
}