package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.standard.ArrivedAtDestinationCheck;
import org.opentripplanner.transit.raptor.rangeraptor.standard.DestinationArrivalListener;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;
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
    private final DebugHandler<ArrivalView<?>> debugHandler;

    private boolean newElementSet;
    private int bestDestinationTime = -1;
    private int bestRound = -1;
    private RaptorTransfer bestEgressPath;

    public EgressArrivalToPathAdapter(
            DestinationArrivalPaths<T> paths,
            TransitCalculator<T> calculator,
            StopsCursor<T> cursor,
            WorkerLifeCycle lifeCycle,
            DebugHandlerFactory<T> debugHandlerFactory

    ) {
        this.paths = paths;
        this.calculator = calculator;
        this.cursor = cursor;
        lifeCycle.onSetupIteration((ignore) -> setupIteration());
        lifeCycle.onRoundComplete((ignore) -> roundComplete());
        this.debugHandler = debugHandlerFactory.debugStopArrival();
    }

    @Override
    public void newDestinationArrival(
            int round, int fromStopArrivalTime, RaptorTransfer egressPath
    ) {
        int arrivalTime = calculator.plusDuration(
                fromStopArrivalTime, egressPath.durationInSeconds()
        );

        if (calculator.isBefore(arrivalTime, bestDestinationTime)) {
            newElementSet = true;
            bestDestinationTime = arrivalTime;
            bestEgressPath = egressPath;
            bestRound = round;
        } else {
            if (debugHandler != null) {
                debugHandler.reject(
                        arrivalState(round, egressPath),
                        arrivalState(bestRound, bestEgressPath),
                        "A better destination arrival time for the current iteration exist: "
                                + TimeUtils.timeToStrLong(bestDestinationTime)
                );
            }
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
        paths.add(arrivalState(bestRound, bestEgressPath), bestEgressPath);
    }

    private ArrivalView<T> arrivalState(int round, RaptorTransfer egress) {
        return cursor.stop(round, egress.stop(), egress.stopReachedOnBoard());
    }
}
