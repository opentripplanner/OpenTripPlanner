package org.opentripplanner.routing.algorithm.transferoptimization.api;

import java.util.function.Supplier;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.path.Path;

/**
 * An OptimizedPath decorates a path returned from Raptor with a transfer-priority-cost and a
 * wait-time-optimized-cost.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPath<T extends RaptorTripSchedule>
  extends Path<T>
  implements TransferOptimized {

  private final int transferPriorityCost;
  private final int waitTimeOptimizedCost;
  private final int breakTieCost;

  public OptimizedPath(RaptorPath<T> originalPath) {
    this(
      originalPath.accessLeg(),
      originalPath.rangeRaptorIterationDepartureTime(),
      originalPath.c1(),
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

  /**
   * A utility function for calculating the priority cost for a transfer. If the {@code transfer
   * exist}, the given supplier is used to get the constrained transfer (can be null) and the cost
   * is calculated. If a transfer does not exist, zero cost is returned.
   */
  public static int priorityCost(boolean transferExist, Supplier<RaptorConstrainedTransfer> txGet) {
    if (transferExist) {
      var tx = txGet.get();
      var c = tx == null ? null : (TransferConstraint) tx.getTransferConstraint();
      return TransferConstraint.cost(c);
    }
    // Return no zero cost if a transfer do not exist
    return TransferConstraint.ZERO_COST;
  }

  @Override
  public int transferPriorityCost() {
    return transferPriorityCost;
  }

  @Override
  public int generalizedCostWaitTimeOptimized() {
    return c1() + waitTimeOptimizedCost;
  }

  @Override
  public int breakTieCost() {
    return breakTieCost;
  }

  @Override
  public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
    return buildString(true, stopNameResolver, this::appendSummary);
  }

  @Override
  public String toString(RaptorStopNameResolver stopNameResolver) {
    return buildString(false, stopNameResolver, this::appendSummary);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  private static int priorityCost(RaptorPath<?> path) {
    return path.legStream().mapToInt(OptimizedPath::priorityCost).sum();
  }

  private static int priorityCost(PathLeg<?> leg) {
    // Only calculate priority cost for transit legs which are followed by at least one
    // other transit leg.
    return priorityCost(
      leg.isTransitLeg() && leg.nextTransitLeg() != null,
      () -> leg.asTransitLeg().getConstrainedTransferAfterLeg()
    );
  }

  private void appendSummary(PathStringBuilder buf) {
    buf.costCentiSec(transferPriorityCost, TransferConstraint.ZERO_COST, "pri");
    buf.costCentiSec(generalizedCostWaitTimeOptimized(), c1(), "wtc");
  }
}
