package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.standard.ArrivedAtDestinationCheck;
import org.opentripplanner.transit.raptor.rangeraptor.standard.DestinationArrivalListener;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.util.time.TimeUtils;


/**
 * The responsibility of this class is to listen for egress stop arrivals and forward these as
 * Destination arrivals to the {@link DestinationArrivalPaths}.
 * <p/>
 * Range Raptor requires paths to be collected at the end of each iteration. Following
 * iterations may overwrite the existing state; Hence invalidate trips explored in previous
 * iterations. Because adding new destination arrivals to the set of paths is expensive,
 * this class optimize this by only adding new destination arrivals at the end of each round.
 * <p/>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class EgressArrivalToPathAdapter<T extends RaptorTripSchedule> implements
        ArrivedAtDestinationCheck, DestinationArrivalListener {
    private final DestinationArrivalPaths<T> paths;
    private final TransitCalculator<T> calculator;
    private final StopsCursor<T> cursor;

    private boolean newElementSet;
    private int bestDestinationTime = -1;
    private int bestRound = -1;
    private boolean bestStopReachedOnBoard = false;
    private RaptorTransfer bestEgressPath;

    public EgressArrivalToPathAdapter(
            DestinationArrivalPaths<T> paths,
            TransitCalculator<T> calculator,
            StopsCursor<T> cursor,
            WorkerLifeCycle lifeCycle
    ) {
        this.paths = paths;
        this.calculator = calculator;
        this.cursor = cursor;
        lifeCycle.onSetupIteration((ignore) -> setupIteration());
        lifeCycle.onRoundComplete((ignore) -> roundComplete());
    }

    @Override
    public void newDestinationArrival(
            int round,
            int fromStopArrivalTime,
            boolean stopReachedOnBoard,
            RaptorTransfer egressPath
    ) {
        int arrivalTime = calculator.plusDuration(
                fromStopArrivalTime, egressPath.durationInSeconds()
        );

        if (calculator.isBefore(arrivalTime, bestDestinationTime)) {
            debugRejectExisting(arrivalTime);
            newElementSet = true;
            bestDestinationTime = arrivalTime;
            bestEgressPath = egressPath;
            bestRound = round;
            bestStopReachedOnBoard = stopReachedOnBoard;
        } else {
            debugRejectNew(round, stopReachedOnBoard, egressPath);
        }
    }

    private void setupIteration() {
        newElementSet = false;
        bestDestinationTime = calculator.unreachedTime();
        bestEgressPath = null;
        bestRound = -1;
    }

    private void roundComplete() {
        if (newElementSet) {
            addNewElementToPath();
            newElementSet = false;
        }
    }

    @Override
    public boolean arrivedAtDestinationCurrentRound() {
        return newElementSet;
    }

    private void addNewElementToPath() {
        paths.add(arrivalState(bestRound, bestStopReachedOnBoard, bestEgressPath), bestEgressPath);
    }

    private void debugRejectNew(int round, boolean stopReachedOnBoard, RaptorTransfer egressPath) {
        if(!paths.isDebugOn()) { return; }
        var arrival = arrivalState(round, stopReachedOnBoard, egressPath);
        debugRejectEvent(arrival, egressPath, bestDestinationTime);
    }

    private void debugRejectExisting(int arrivalTime) {
        if(!paths.isDebugOn() || !newElementSet) { return; }
        var arrival = arrivalState(bestRound, bestStopReachedOnBoard, bestEgressPath);
        debugRejectEvent(arrival, bestEgressPath, arrivalTime);
    }

    private void debugRejectEvent(ArrivalView<T> arrival, RaptorTransfer egress, int arrivalTime) {
        String reason = String.format(
                "Better arrival time exist: %s  [PathAdaptor]",
                TimeUtils.timeToStrLong(arrivalTime)
        );
        paths.debugReject(arrival, egress, reason);
    }

    private ArrivalView<T> arrivalState(int round, boolean stopReachedOnBoard, RaptorTransfer egress) {
        return cursor.stop(round, egress.stop(), stopReachedOnBoard);
    }
}
