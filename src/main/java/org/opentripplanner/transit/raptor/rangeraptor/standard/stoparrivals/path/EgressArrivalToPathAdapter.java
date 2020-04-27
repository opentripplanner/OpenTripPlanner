package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.standard.ArrivedAtDestinationCheck;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.EgressStopArrivalState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;
import org.opentripplanner.transit.raptor.util.TimeUtils;


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
public class EgressArrivalToPathAdapter<T extends RaptorTripSchedule> implements ArrivedAtDestinationCheck {
    private final DestinationArrivalPaths<T> paths;
    private final TransitCalculator calculator;
    private final StopsCursor<T> cursor;
    private final DebugHandler<ArrivalView<T>> debugHandler;

    private boolean newElementSet;
    private EgressStopArrivalState<T> bestEgressStopArrival = null;
    private int bestDestinationTime = -1;

    public EgressArrivalToPathAdapter(
            DestinationArrivalPaths<T> paths,
            TransitCalculator calculator,
            StopsCursor<T> cursor,
            WorkerLifeCycle lifeCycle,
            DebugHandlerFactory<T> debugHandlerFactory

    ) {
        this.paths = paths;
        this.calculator = calculator;
        this.cursor = cursor;
        lifeCycle.onSetupIteration((ignore) -> setupIteration());
        lifeCycle.onRoundComplete((ignore) -> roundComplete());

        debugHandler = debugHandlerFactory.debugStopArrival();
    }

    public void add(EgressStopArrivalState<T> egressStopArrival) {
        // TODO: Check earliestDepartureTime?
        int time = destinationArrivalTime(egressStopArrival);
        if (calculator.isBest(time, bestDestinationTime)) {
            newElementSet = true;
            bestDestinationTime = time;
            bestEgressStopArrival = egressStopArrival;
        } else {
            if (debugHandler != null) {
                debugHandler.reject(
                        cursor.stop(egressStopArrival.round(), egressStopArrival.stop()),
                        cursor.stop(bestEgressStopArrival.round(), bestEgressStopArrival.stop()),
                        "A better destination arrival time for the current iteration exist: "
                                + TimeUtils.timeToStrLong(bestDestinationTime)
                );
            }
        }
    }

    private void setupIteration() {
        newElementSet = false;
        bestEgressStopArrival = null;
        bestDestinationTime = calculator.unreachedTime();
    }

    private void roundComplete() {
        if (newElementSet) {
            addToPath(bestEgressStopArrival);
            newElementSet = false;
        }
    }

    private int destinationArrivalTime(EgressStopArrivalState<T> arrival) {
        return calculator.plusDuration(arrival.transitTime(), arrival.egressLeg().durationInSeconds());
    }

    @Override
    public boolean arrivedAtDestinationCurrentRound() {
        return newElementSet;
    }

    private void addToPath(final EgressStopArrivalState<T> it) {
        paths.add(cursor.transit(it.round(), it.stop()), it.egressLeg(), 0);
    }
}
