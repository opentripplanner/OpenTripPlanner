package org.opentripplanner.transit.raptor.rangeraptor.path;

import java.util.Collection;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;

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
    private final TransitCalculator<T> transitCalculator;
    private final CostCalculator<T> costCalculator;
    private final SlackProvider slackProvider;
    private final PathMapper<T> pathMapper;
    private final DebugHandler<ArrivalView<?>> debugHandler;
    private boolean reachedCurrentRound = false;
    private int iterationDepartureTime = -1;

    public DestinationArrivalPaths(
            ParetoComparator<Path<T>> paretoComparator,
            TransitCalculator<T> transitCalculator,
            CostCalculator<T> costCalculator,
            SlackProvider slackProvider,
            PathMapper<T> pathMapper,
            DebugHandlerFactory<T> debugHandlerFactory,
            WorkerLifeCycle lifeCycle
    ) {
        this.paths = new ParetoSet<>(paretoComparator, debugHandlerFactory.paretoSetDebugPathListener());
        this.transitCalculator = transitCalculator;
        this.costCalculator = costCalculator;
        this.slackProvider = slackProvider;
        this.pathMapper = pathMapper;
        this.debugHandler = debugHandlerFactory.debugStopArrival();
        lifeCycle.onPrepareForNextRound(round -> clearReachedCurrentRoundFlag());
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    public void add(
        ArrivalView<T> egressStopArrival,
        RaptorTransfer egressPath,
        int aggregatedCost
    ) {
        int departureTime = transitCalculator.departureTime(
            egressPath,
            egressStopArrival.arrivalTime()
        );

        if (departureTime == -1) { return; }

        if(egressPath.hasRides()) {
            departureTime = transitCalculator.plusDuration(
                departureTime,
                slackProvider.accessEgressWithRidesTransferSlack()
            );
        }

        int arrivalTime = transitCalculator.plusDuration(
            departureTime,
            egressPath.durationInSeconds()
        );

        int waitTimeInSeconds = Math.abs(departureTime - egressStopArrival.arrivalTime());

        // If the aggregatedCost is zero(StdRaptor), then cost calculation is skipped.
        // If the aggregatedCost exist(McRaptor), then the cost of waiting is added.
        int cost = aggregatedCost == 0 ? 0 : aggregatedCost + costCalculator.waitCost(waitTimeInSeconds);

        DestinationArrival<T> destArrival = new DestinationArrival<>(
            egressPath,
            egressStopArrival,
            arrivalTime,
            cost
        );

        if (transitCalculator.exceedsTimeLimit(arrivalTime)) {
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

    public void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    public boolean qualify(int departureTime, int arrivalTime, int numberOfTransfers, int cost) {
        return paths.qualify(Path.dummyPath(iterationDepartureTime, departureTime, arrivalTime, numberOfTransfers, cost));
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
            debugHandler.reject(destArrival.previous(), null, transitCalculator.exceedsTimeLimitReason());
        }
    }
}