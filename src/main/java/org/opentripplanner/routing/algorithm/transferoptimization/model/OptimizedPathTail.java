package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Map;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimized;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * This class is used to decorate a {@link TransitPathLeg} with information about the
 * guaranteed-transfers and cashing transfer-priority-cost and optimized-wait-time-transfer-cost.
 * <p>
 * The class is only used inside the {@code transferoptimization} package to store temporary
 * path "tails", while building new paths with new transfer points.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPathTail<T extends RaptorTripSchedule> implements TransferOptimized {

    private final TransitPathLeg<T> leg;
    private final Map<PathLeg<T>, Transfer> transfersTo;
    private final int transferPriorityCost;
    private final int waitTimeOptimizedCost;

    public OptimizedPathTail(
            TransitPathLeg<T> leg,
            Map<PathLeg<T>, Transfer> transfersTo,
            int transferPriorityCost,
            int waitTimeOptimizedCost
    ) {
        this.transfersTo = transfersTo;
        this.leg = leg;
        this.transferPriorityCost = transferPriorityCost;
        this.waitTimeOptimizedCost = waitTimeOptimizedCost;
    }

    public TransitPathLeg<T> getLeg() {
        return leg;
    }

    /**
     * A map of all guaranteed transfers for all transfers part of this tail. The potential
     * set of keys are the transfer legs part of this tail, but a transfer-leg/guaranteed-transfer
     * (key/value) is only added if the guaranteed-transfer exist.
     */
    public Map<PathLeg<T>, Transfer> getTransfersTo() {
        return transfersTo;
    }

    @Override
    public int waitTimeOptimizedCost() {
        return waitTimeOptimizedCost;
    }

    @Override
    public int transferPriorityCost() {
        return transferPriorityCost;
    }

    @Override
    public int breakTieCost() {
        // We add the arrival-times together to mimic doing the transfers as early as possible
        // when there are more than on transfer point between two trips.
        // We calculate this on the fly, because it is not likely to be done very often and
        // the calculation is light-weight
        return leg.stream().filter(PathLeg::isTransitLeg).mapToInt(PathLeg::toTime).sum();
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(OptimizedPathTail.class)
                .addNum("transferPriorityCost", transferPriorityCost)
                .addNum("waitTimeOptimizedCost", waitTimeOptimizedCost)
                .addObj("leg", leg)
                .addObj("transfersTo", transfersTo)
                .toString();
    }
}
