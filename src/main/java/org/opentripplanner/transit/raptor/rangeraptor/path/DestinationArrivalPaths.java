package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;

import java.util.Collection;

/**
 * The responsibility of this class is to collect result paths for destination arrivals.
 * It does so using a pareto set. The comparator is passed in as an argument to the
 * constructor. This make is possible to collect different sets in different scenarios.
 * <p/>
 * Depending on the pareto comparator passed into the constructor this class grantee that the
 * best paths with respect to <em>arrival time</em>, <em>rounds</em> and <em>travel duration</em>
 * are found. You may also add <em>cost</em> as a criteria (multi-criteria search).
 * <p/>
 * This class is a thin wrapper around a ParetoSet of {@link Path}s. Before paths are added the
 * arrival time is checked against the arrival time limit.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DestinationArrivalPaths<T extends RaptorTripSchedule> {
    private final ParetoSet<Path<T>> paths;
    private final TransitCalculator calculator;
    private final PathMapper<T> pathMapper;
    private final DebugHandler<ArrivalView<T>> debugHandler;
    private boolean reachedCurrentRound = false;

    public DestinationArrivalPaths(
            ParetoComparator<Path<T>> paretoComparator,
            TransitCalculator calculator,
            DebugHandlerFactory<T> debugHandlerFactory,
            WorkerLifeCycle lifeCycle
    ) {
        this.paths = new ParetoSet<>(paretoComparator, debugHandlerFactory.paretoSetDebugPathListener());
        this.debugHandler = debugHandlerFactory.debugStopArrival();
        this.calculator = calculator;
        this.pathMapper = calculator.createPathMapper();
        lifeCycle.onPrepareForNextRound(round -> clearReachedCurrentRoundFlag());
    }

    public void add(ArrivalView<T> egressStopArrival, RaptorTransfer egressLeg, int additionalCost) {
        int arrivalTime = calculator.plusDuration(egressStopArrival.arrivalTime(), egressLeg.durationInSeconds());
        DestinationArrival<T> destArrival = new DestinationArrival<>(egressStopArrival, arrivalTime, additionalCost);

        if (calculator.exceedsTimeLimit(arrivalTime)) {
            debugRejectByTimeLimitOptimization(destArrival);
        } else {
            Path<T> path = pathMapper.mapToPath(destArrival);
            boolean added = paths.add(path);
            if (added) {
                reachedCurrentRound = true;
            }
        }
    }

    /**
     * Check if destination was reached in the current round.
     */
    public boolean isReachedCurrentRound() {
        return reachedCurrentRound;
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    public boolean qualify(int departureTime, int arrivalTime, int numberOfTransfers, int cost) {
        return paths.qualify(Path.dummyPath(departureTime, arrivalTime, numberOfTransfers, cost));
    }

    public Collection<Path<T>> listPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return paths.toString();
    }


    /* private methods */

    private void clearReachedCurrentRoundFlag() {
        reachedCurrentRound = false;
    }

    private void debugRejectByTimeLimitOptimization(DestinationArrival<T> destArrival) {
        if (debugHandler != null) {
            debugHandler.reject(destArrival.previous(), null, calculator.exceedsTimeLimitReason());
        }
    }
}