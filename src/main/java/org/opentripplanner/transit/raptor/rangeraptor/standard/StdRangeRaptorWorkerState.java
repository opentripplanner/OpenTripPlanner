package org.opentripplanner.transit.raptor.rangeraptor.standard;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.util.BitSetIterator;

import java.util.Collection;
import java.util.Iterator;


/**
 * Tracks the state of a standard Range Raptor search, specifically the best arrival times at each transit stop
 * at the end of a particular round, along with associated data to reconstruct paths etc.
 * <p>
 * This is grouped into a separate class (rather than just having the fields in the raptor worker class) because we
 * want to separate the logic of maintaining stop arrival state and performing the steps of the algorithm. This
 * also make it possible to have more than one state implementation, which have ben used in the past to test different
 * memory optimizations.
 * <p>
 * Note that this represents the entire state of the Range Raptor search for all rounds. The {@code stopArrivalsState}
 * implementation can be swapped to achieve different results.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdRangeRaptorWorkerState<T
        extends RaptorTripSchedule>
        implements StdWorkerState<T>
{

    /**
     * The best times to reach each stop, whether via a transfer or via transit directly.
     * This is the bare minimum to execute the algorithm.
     */
    private final BestTimes bestTimes;

    /**
     * Track the stop arrivals to be able to return some kind of result. Depending on the
     * desired result, different implementation is injected.
     */
    private final StopArrivalsState<T> stopArrivalsState;

    /**
     * The list of egress stops, can be used to terminate the search when the stops are reached.
     */
    private final ArrivedAtDestinationCheck arrivedAtDestinationCheck;


    /**
     * The calculator is used to calculate transit related times/events like access arrival time.
     */
    private final TransitCalculator calculator;

    /**
     * create a BestTimes Range Raptor State for given context.
     */
    public StdRangeRaptorWorkerState(
            TransitCalculator calculator,
            BestTimes bestTimes,
            StopArrivalsState<T> stopArrivalsState,
            ArrivedAtDestinationCheck arrivedAtDestinationCheck
    ) {
        this.calculator = calculator;
        this.bestTimes = bestTimes;
        this.stopArrivalsState = stopArrivalsState;
        this.arrivedAtDestinationCheck = arrivedAtDestinationCheck;
    }

    @Override
    public final void setInitialTimeForIteration(RaptorTransfer accessEgressLeg, int departureTime) {
        int durationInSeconds = accessEgressLeg.durationInSeconds();
        int stop = accessEgressLeg.stop();

        // The time of arrival at the given stop for the current iteration
        // (or departure time at the last stop if we search backwards).
        int arrivalTime = calculator.plusDuration(departureTime, durationInSeconds);

        bestTimes.setAccessStopTime(stop, arrivalTime);
        stopArrivalsState.setAccess(stop, arrivalTime, accessEgressLeg);
    }

    @Override
    public final boolean isNewRoundAvailable() {
        return bestTimes.isCurrentRoundUpdated();
    }

    @Override
    public final BitSetIterator stopsTouchedByTransitCurrentRound() {
        return bestTimes.transitStopsReachedCurrentRound();
    }

    @Override
    public final IntIterator stopsTouchedPreviousRound() {
        return bestTimes.stopsReachedLastRound();
    }


    @Override
    public final boolean isStopReachedInPreviousRound(int stop) {
        return bestTimes.isStopReachedLastRound(stop);
    }

    /**
     * Return the "best time" found in the previous round. This is used to calculate the board/alight
     * time in the next round.
     * <p/>
     * PLEASE OVERRIDE!
     * <p/>
     * The implementation here is not correct - please override if you plan to use any result paths
     * or "rounds" as "number of transfers". The implementation is OK if the only thing you care
     * about is the "arrival time".
     */
    @Override
    public int bestTimePreviousRound(int stop) {
        // This is a simplification, *bestTimes* might get updated during the current round;
        // Hence leading to a new boarding from the same stop in the same round.
        // If we do not count rounds or track paths, this is OK. But be sure to override this
        // method with the best time from the previous round if you care about number of
        // transfers and results paths.

        return stopArrivalsState.bestTimePreviousRound(stop);
    }

    /**
     * Set the time at a transit stop iff it is optimal. This sets both the bestTime and the transitTime.
     */
    @Override
    public final void transitToStop(int stop, int alightTime, int boardStop, int boardTime, T trip) {
        if (exceedsTimeLimit(alightTime)) {
            return;
        }

        if (newTransitBestTime(stop, alightTime)) {
            // transitTimes upper bounds bestTimes
            final boolean newBestOverall = newOverallBestTime(stop, alightTime);
            stopArrivalsState.setNewBestTransitTime(stop, alightTime, trip, boardStop, boardTime, newBestOverall);
        } else {
            stopArrivalsState.rejectNewBestTransitTime(stop, alightTime, trip, boardStop, boardTime);
        }
    }

    /**
     * Set the arrival time at all transit stop if time is optimal for the given list of transfers.
     */
    @Override
    public final void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
        int arrivalTimeTransit = bestTimes.transitTime(fromStop);
        while (transfers.hasNext()) {
            transferToStop(arrivalTimeTransit, fromStop, transfers.next());
        }
    }

    @Override
    public Collection<Path<T>> extractPaths() {
        return stopArrivalsState.extractPaths();
    }

    private void transferToStop(int arrivalTimeTransit, int fromStop, RaptorTransfer transferLeg) {
        // Use the calculator to make sure the calculation is done correct for a normal
        // forward search and a reverse search.
        final int arrivalTime = calculator.plusDuration(arrivalTimeTransit, transferLeg.durationInSeconds());

        if (exceedsTimeLimit(arrivalTime)) {
            return;
        }

        final int toStop = transferLeg.stop();

        // transitTimes upper bounds bestTimes so we don't need to update wait time and in-vehicle time here, if we
        // enter this conditional it has already been updated.
        if (newOverallBestTime(toStop, arrivalTime)) {
            stopArrivalsState.setNewBestTransferTime(fromStop, arrivalTime, transferLeg);
        } else {
            stopArrivalsState.rejectNewBestTransferTime(fromStop, arrivalTime, transferLeg);
        }
    }

    @Override
    public boolean isDestinationReachedInCurrentRound() {
        return arrivedAtDestinationCheck.arrivedAtDestinationCurrentRound();
    }


    /* private methods */

    private boolean newTransitBestTime(int stop, int alightTime) {
        return bestTimes.transitUpdateNewBestTime(stop, alightTime);
    }

    private boolean newOverallBestTime(int stop, int alightTime) {
        return bestTimes.updateNewBestTime(stop, alightTime);
    }

    private boolean exceedsTimeLimit(int time) {
        return calculator.exceedsTimeLimit(time);
    }
}