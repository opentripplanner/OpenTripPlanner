package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class OptimizedPathFactory<T extends RaptorTripSchedule> {

    private final ToIntFunction<PathLeg<?>> costCalcForWaitOptimization;

    public OptimizedPathFactory(ToIntFunction<PathLeg<?>> costCalcForWaitOptimization) {
        this.costCalcForWaitOptimization = costCalcForWaitOptimization;
    }


    OptimizedPath<T> createPath(
            Path<T> originalPath,
            OptimizedPathTail<T> tail
    ) {
        var accessPathLeg = new AccessPathLeg<>(originalPath.accessLeg(), tail.getLeg());
        return new OptimizedPath<>(
                originalPath,
                new Path<>(originalPath.rangeRaptorIterationDepartureTime(), accessPathLeg),
                tail.getTransfersTo(),
                tail.transferPriorityCost(),
                costCalcForWaitOptimization.applyAsInt(accessPathLeg),
                tail.breakTieCost()
        );
    }

    /**
     * Create new tail for the last transit leg in the path.
     */
    OptimizedPathTail<T> createPathLeg(TransitPathLeg<T> leg) {
        return new OptimizedPathTail<>(
                leg,
                Map.of(),
                Transfer.NEUTRAL_PRIORITY_COST,
                costCalcForWaitOptimization.applyAsInt(leg)
        );
    }

    /**
     * Create new tail for a transit-leg connected to the given transfer with the given transfer.
     */
    OptimizedPathTail<T> createPathLeg(
            TransitPathLeg<T> leg,
            @Nullable Transfer tx,
            Map<PathLeg<T>, Transfer> transfersTo,
            TransitPathLeg<T> toTransitLeg
    ) {
        var transfers = createTransfers(toTransitLeg, tx, transfersTo);
        return new OptimizedPathTail<>(
                leg,
                transfers,
                calculateTxPriorityCost(transfers),
                costCalcForWaitOptimization.applyAsInt(leg)
        );
    }

    /* private methods */

    private static <T extends RaptorTripSchedule> Map<PathLeg<T>, Transfer> createTransfers(
            TransitPathLeg<T> leg,
            Transfer tx,
            Map<PathLeg<T>, Transfer> transfers
    ) {
        if(tx == null) {
            return transfers;
        }
        if(transfers == null) {
            return Map.of(leg, tx);
        }
        // The tail can be part of more then one path, so we need to copy the
        // tail transfers before adding the current leg to it
        var map = new HashMap<>(transfers);
        map.put(leg, tx);
        return map;
    }

    /**
     * Return the total transfer priority cost. This is completely separate from the
     * generalized-cost. Return zero if the cost is neutral (no "special" transfers exist).
     *
     * @see Transfer#priorityCost(Transfer)
     */
    private int calculateTxPriorityCost(Map<?, Transfer> transfers) {
        if(transfers == null) { return 0; }

        return transfers.values().stream().mapToInt(Transfer::priorityCost).sum();
    }
}
