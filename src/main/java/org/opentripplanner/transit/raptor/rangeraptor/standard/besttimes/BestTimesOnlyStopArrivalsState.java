package org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StopArrivalsState;

import java.util.Collection;
import java.util.Collections;

/**
 * The responsibility of this class is to calculate the best arrival times at every stop.
 * This class do NOT keep track of results paths.
 * <p/>
 * The {@link #bestTimePreviousRound(int)} return an estimate of the best time for the
 * previous round by using the overall best time (any round including the current round).
 * <p/>
 * This class is used to calculate heuristic information like the best possible arrival times
 * and the minimum number for transfers. The results are an optimistic "guess", since we uses
 * the overall best time instead of best time previous round we might skip hops.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class BestTimesOnlyStopArrivalsState<T extends RaptorTripSchedule> implements StopArrivalsState<T> {

    private final BestTimes bestTimes;
    private final SimpleBestNumberOfTransfers bestNumberOfTransfers;


    public BestTimesOnlyStopArrivalsState(BestTimes bestTimes, SimpleBestNumberOfTransfers bestNumberOfTransfers) {
        this.bestTimes = bestTimes;
        this.bestNumberOfTransfers = bestNumberOfTransfers;
    }

    @Override
    public void setAccess(int stop, int arrivalTime, RaptorTransfer access) {
        bestNumberOfTransfers.arriveAtStop(stop);
    }

    @Override
    public Collection<Path<T>> extractPaths() { return Collections.emptyList(); }

    /**
     * This implementation does NOT return the "best time in the previous round"; It returns the
     * overall "best time" across all rounds including the current.
     * <p/>
     * This is a simplification, *bestTimes* might get updated during the current round; Hence
     * leading to a new boarding at the alight stop in the same round. If we do not count rounds
     * or track paths, this is OK.
     * <P/>
     * Because this rarely happens and heuristics does not need to be exact - it only need to be
     * optimistic. So if we arrive at a stop one or two rounds to early, the only effect is that
     * the "number of transfers" for those stops is to small - or what we call a optimistic estimate.
     * <p/>
     * The "arrival time" is calculated correctly.
     */
    @Override
    public int bestTimePreviousRound(int stop) { return bestTimes.time(stop); }

    @Override
    public void setNewBestTransitTime(int stop, int alightTime, T trip, int boardStop, int boardTime, boolean newBestOverall) {
        bestNumberOfTransfers.arriveAtStop(stop);
    }

    @Override
    public void rejectNewBestTransitTime(int stop, int alightTime, T trip, int boardStop, int boardTime) { }

    @Override
    public void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transferLeg) {
        bestNumberOfTransfers.arriveAtStop(transferLeg.stop());
    }

    @Override
    public void rejectNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transferLeg) { }
}
