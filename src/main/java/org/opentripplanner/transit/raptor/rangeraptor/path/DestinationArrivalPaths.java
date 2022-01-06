package org.opentripplanner.transit.raptor.rangeraptor.path;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.OtpNumberFormat;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
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
import org.opentripplanner.util.logging.ThrottleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(DestinationArrivalPaths.class);
    private static final Logger LOG_MISS_MATCH = ThrottleLogger.throttle(LOG);

    private final ParetoSet<Path<T>> paths;
    private final TransitCalculator<T> transitCalculator;
    @Nullable
    private final CostCalculator costCalculator;
    private final SlackProvider slackProvider;
    private final PathMapper<T> pathMapper;
    private final DebugHandler<ArrivalView<?>> debugHandler;
    private final RaptorStopNameResolver stopNameResolver;
    private boolean reachedCurrentRound = false;
    private int iterationDepartureTime = -1;

    public DestinationArrivalPaths(
            ParetoComparator<Path<T>> paretoComparator,
            TransitCalculator<T> transitCalculator,
            @Nullable CostCalculator costCalculator,
            SlackProvider slackProvider,
            PathMapper<T> pathMapper,
            DebugHandlerFactory<T> debugHandlerFactory,
            RaptorStopNameResolver stopNameResolver,
            WorkerLifeCycle lifeCycle
    ) {
        this.paths = new ParetoSet<>(paretoComparator, debugHandlerFactory.paretoSetDebugPathListener());
        this.transitCalculator = transitCalculator;
        this.costCalculator = costCalculator;
        this.slackProvider = slackProvider;
        this.pathMapper = pathMapper;
        this.debugHandler = debugHandlerFactory.debugStopArrival();
        this.stopNameResolver = stopNameResolver;
        lifeCycle.onPrepareForNextRound(round -> clearReachedCurrentRoundFlag());
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    public void add(
        ArrivalView<T> stopArrival,
        RaptorTransfer egressPath
    ) {
        int departureTime = transitCalculator.departureTime(egressPath, stopArrival.arrivalTime());

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

        int waitTimeInSeconds = Math.abs(departureTime - stopArrival.arrivalTime());

        // If the aggregatedCost is zero(StdRaptor), then cost calculation is skipped.
        // If the aggregatedCost exist(McRaptor), then the cost of waiting is added.
        int additionalCost = 0;

        if(costCalculator != null) {
            additionalCost += costCalculator.waitCost(waitTimeInSeconds);
            additionalCost += costCalculator.costEgress(egressPath);
        }

        DestinationArrival<T> destArrival = new DestinationArrival<>(
            egressPath,
            stopArrival,
            arrivalTime,
            additionalCost
        );

        if (transitCalculator.exceedsTimeLimit(arrivalTime)) {
            debugRejectByTimeLimitOptimization(destArrival);
        } else {
            Path<T> path = pathMapper.mapToPath(destArrival);

            assertGeneralizedCostIsCalculatedCorrectByMapper(destArrival, path);

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
        return paths.toString((p) -> p.toString(stopNameResolver));
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

    /**
     * If the total cost generated by the mapper is not equal to the total cost calculated by
     * Raptor, there is probably a mistake in the mapper! This is a rather critical error and
     * should be fixed. To avoid dropping legal paths from the result set, we log this as an error
     * and allow the path to be included in the result!!!
     * <p>
     * The path mapper might not map the cost to each leg exactly as the Raptor does but the
     * total should be the same. Raptor only have stop-arrival, while the path have legs. A transit
     * leg alight BEFORE the transit stop arrival due to alight-slack.
     */
    private void assertGeneralizedCostIsCalculatedCorrectByMapper(DestinationArrival<T> destArrival, Path<T> path) {
        if(path.generalizedCost() != destArrival.cost()) {
            // TODO - Bug: Cost mismatch stop-arrivals and paths #3623
            LOG_MISS_MATCH.warn(
                    "Cost mismatch - Mapper: {}, stop-arrivals: {}, path: {}",
                    OtpNumberFormat.formatCost(path.generalizedCost()),
                    raptorCostsAsString(destArrival),
                    path.toStringDetailed(stopNameResolver)
            );
        }
    }

    /**
     * Return the cost of all stop arrivals including the destination in reverse order:
     * <p>
     * {@code $1200 $950 $600} (Egress, bus, and access arrival)
     */
    private String raptorCostsAsString(DestinationArrival<T> destArrival) {
        var arrivalCosts = new ArrayList<String>();
        ArrivalView<?> it = destArrival;
        while (it != null) {
            arrivalCosts.add(OtpNumberFormat.formatCost(it.cost()));
            it = it.previous();
        }
        // Remove decimals if zero
        return String.join(" ", arrivalCosts).replaceAll("\\.00", "");
    }
}