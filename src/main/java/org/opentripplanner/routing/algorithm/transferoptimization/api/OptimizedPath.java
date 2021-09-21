package org.opentripplanner.routing.algorithm.transferoptimization.api;

import java.util.function.IntFunction;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;


/**
 * An OptimizedPath decorates a path returned from Raptor with a transfer-priority-cost and
 * a wait-time-optimized-cost.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPath<T extends RaptorTripSchedule> extends Path<T>
        implements TransferOptimized
{
    private final int transferPriorityCost;
    private final int waitTimeOptimizedCost;
    private final int breakTieCost;

    public OptimizedPath(Path<T> originalPath) {
        this(
                originalPath.accessLeg(),
                originalPath.rangeRaptorIterationDepartureTime(),
                originalPath.generalizedCost(),
                priorityCost(originalPath),
                NEUTRAL_COST,
                NEUTRAL_COST
        );
    }

    public OptimizedPath(
            AccessPathLeg<T> accessPathLeg,
            int iterationStartTime,
            int generalizedCost,
            int transferPriorityCost,
            int waitTimeOptimizedCost,
            int breakTieCost
    ) {
        super(iterationStartTime, accessPathLeg, generalizedCost);
        this.transferPriorityCost = transferPriorityCost;
        this.waitTimeOptimizedCost = waitTimeOptimizedCost;
        this.breakTieCost = breakTieCost;
    }

    @Override
    public int transferPriorityCost() {
        return transferPriorityCost;
    }

    @Override
    public int waitTimeOptimizedCost() {
        return waitTimeOptimizedCost;
    }

    @Override
    public int breakTieCost() {
        return breakTieCost;
    }

    @Override
    public String toStringDetailed(IntFunction<String> stopNameTranslator) {
        return buildString(true, stopNameTranslator, this::appendSummary);
    }

    @Override
    public String toString(IntFunction<String> stopNameTranslator) {
        return buildString(false, stopNameTranslator, this::appendSummary);
    }

    @Override
    public String toString() {
        return toString(null);
    }

    private void appendSummary(PathStringBuilder buf) {
        buf.costCentiSec(transferPriorityCost, "pri");
        buf.costCentiSec(waitTimeOptimizedCost, "wtc");
    }

    private static int priorityCost(Path<?> path) {
        return path.legStream().mapToInt(OptimizedPath::priorityCost).sum();
    }

    private static int priorityCost(PathLeg<?> leg) {
        var c = leg.isTransitLeg()
                ? (TransferConstraint) leg.asTransitLeg().getConstrainedTransferAfterLeg()
                : null;
        return TransferConstraint.priorityCost(c);
    }
}
