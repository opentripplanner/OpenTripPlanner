package org.opentripplanner.routing.algorithm.transferoptimization.api;

import java.util.Map;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class OptimizedPath<T extends RaptorTripSchedule> extends Path<T> {
    private static final int NOT_SET = -999_999;
    private final Path<T> originalPath;

    private final int waitTimeOptimizedCost;
    private final Map<PathLeg<T>, Transfer> transfers;

    public OptimizedPath(Path<T> original, Path<T> newPath) {
        super(newPath);
        this.originalPath = original;
        this.transfers = Map.of();
        this.waitTimeOptimizedCost = NOT_SET;
    }

    public OptimizedPath(OptimizedPath<T> original, Map<PathLeg<T>, Transfer> transfers) {
        super(original);
        this.originalPath = original.originalPath;
        this.transfers = Map.copyOf(transfers);
        this.waitTimeOptimizedCost = original.waitTimeOptimizedCost;
    }

    private OptimizedPath(OptimizedPath<T> original, int waitTimeOptimizedCost) {
        super(original);
        this.originalPath = original.originalPath;
        this.transfers = original.transfers;
        this.waitTimeOptimizedCost = waitTimeOptimizedCost;
    }

    public OptimizedPath<T> withTransfers(Map<PathLeg<T>, Transfer> transfers) {
        return new OptimizedPath<>(this, transfers);
    }

    public OptimizedPath<T> withWaitTimeCost(int newWaitTimeCost) {
        return new OptimizedPath<>(this, newWaitTimeCost);
    }


    /** Return the total transfer priority cost. This have nothing to do with the
     * generalized-cost. Return {@code 0}(zero) if cost is neutral/no "special"-transfers exist.
     *
     * @see Transfer#priorityCost(Transfer)
     */
    public int transferPriorityCost() {
        return transfers.values().stream().mapToInt(Transfer::priorityCost).sum();
    }

    public int getWaitTimeOptimizedCost() {
        return waitTimeOptimizedCost;
    }

    public Transfer getTransferTo(PathLeg<?> leg) {
        return transfers.get(leg);
    }
}
