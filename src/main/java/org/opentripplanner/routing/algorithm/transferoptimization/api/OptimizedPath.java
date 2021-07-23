package org.opentripplanner.routing.algorithm.transferoptimization.api;

import java.util.Map;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * An OptimizedPath decorates a path returned from Raptor with a transfer-priority-cost and
 * a wait-time-optimized-cost.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPath<T extends RaptorTripSchedule> extends Path<T>
        implements TransferOptimized
{


    private final Path<T> originalPath;
    private final Map<PathLeg<T>, Transfer> transfersTo;
    private final int transferPriorityCost;
    private final int waitTimeOptimizedCost;
    private final int breakTieCost;


    public OptimizedPath(Path<T> originalPath) {
        this(
                originalPath,
                originalPath,
                Map.of(),
                Transfer.NEUTRAL_PRIORITY_COST,
                NEUTRAL_COST,
                NEUTRAL_COST
        );
    }

    public OptimizedPath(
            Path<T> originalPath,
            Path<T> path,
            Map<PathLeg<T>, Transfer> transfersTo,
            int transferPriorityCost,
            int waitTimeOptimizedCost,
            int breakTieCost
    ) {
        super(path);
        this.originalPath = originalPath;
        this.transfersTo = transfersTo;
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

    public Transfer getTransferTo(PathLeg<?> leg) {
        return transfersTo.get(leg);
    }

    public boolean isSameAsOriginal() {
        PathLeg<T> originalLeg = originalPath.accessLeg();
        PathLeg<T> newLeg = accessLeg();

        while (!originalLeg.isEgressLeg() && !newLeg.isEgressLeg()) {
            if(originalLeg.toStop() != newLeg.toStop()) {
                return false;
            }
            originalLeg = originalLeg.nextLeg();
            newLeg = newLeg.nextLeg();
        }
        return true;
    }
}
