package org.opentripplanner.routing.algorithm.transferoptimization.model;

import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorValueFormatter;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimized;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * This class is used to decorate a {@link TransitPathLeg} with information about transfers
 * constraints, and also caches transfer-priority-cost and optimized-wait-time-transfer-cost.
 * <p>
 * The class is only used inside the {@code transferoptimization} package to store temporary path
 * "tails", while building new paths with new transfer points.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPathTail<T extends RaptorTripSchedule>
  extends PathBuilder<T>
  implements TransferOptimized {

  @Nullable
  private final TransferWaitTimeCostCalculator waitTimeCostCalculator;

  @Nullable
  private final StopPriorityCostCalculator stopPriorityCostCalculator;

  private int transferPriorityCost = TransferConstraint.ZERO_COST;
  private int waitTimeOptimizedCost = TransferWaitTimeCostCalculator.ZERO_COST;
  private int generalizedCost = RaptorCostCalculator.ZERO_COST;

  public OptimizedPathTail(
    RaptorSlackProvider slackProvider,
    RaptorCostCalculator<T> costCalculator,
    int iterationDepartureTime,
    TransferWaitTimeCostCalculator waitTimeCostCalculator,
    @Nullable int[] stopBoardAlightTransferCosts,
    double extraStopBoardAlightCostsFactor,
    RaptorStopNameResolver stopNameResolver
  ) {
    super(slackProvider, iterationDepartureTime, costCalculator, stopNameResolver, null);
    this.waitTimeCostCalculator = waitTimeCostCalculator;
    this.stopPriorityCostCalculator = (stopBoardAlightTransferCosts != null &&
        extraStopBoardAlightCostsFactor > 0.01)
      ? new StopPriorityCostCalculator(
        extraStopBoardAlightCostsFactor,
        stopBoardAlightTransferCosts
      )
      : null;
  }

  private OptimizedPathTail(OptimizedPathTail<T> other) {
    super(other);
    this.waitTimeCostCalculator = other.waitTimeCostCalculator;
    this.waitTimeOptimizedCost = other.waitTimeOptimizedCost;
    this.transferPriorityCost = other.transferPriorityCost;
    this.stopPriorityCostCalculator = other.stopPriorityCostCalculator;
    this.generalizedCost = other.generalizedCost;
  }

  /**
   * Create a deep-copy of this builder.
   */
  public OptimizedPathTail<T> mutate() {
    return new OptimizedPathTail<>(this);
  }

  /** Start by adding the last transit leg with the egress leg attached. */
  public OptimizedPathTail<T> addTransitTail(TransitPathLeg<T> leg) {
    var next = leg.nextLeg();
    RaptorTransfer transfer = null;
    // this can also be a transfer leg to a flex trip
    if (next.isTransferLeg()) {
      transfer = next.asTransferLeg().transfer();
      next = next.nextLeg();
    }
    if (next.isEgressLeg()) {
      egress(next.asEgressLeg().egress());
      if (transfer != null) {
        transfer(transfer, transfer.stop());
      }
      var times = new BoardAndAlightTime(
        leg.trip(),
        leg.getFromStopPosition(),
        leg.getToStopPosition()
      );
      transit(leg.trip(), times);
    } else {
      throw new IllegalStateException("We expect an egress leg at the end of the RAPTOR path.");
    }
    return this;
  }

  /**
   * Insert a new transit leg at the head and return the new object. The new tail is returned with
   * the given transit + transfer leg, earliest-departure-time and the current leg as a new tail.
   */
  public OptimizedPathTail<T> addTransitAndTransferLeg(
    TransitPathLeg<T> originalLeg,
    TripToTripTransfer<T> tx
  ) {
    head().changeBoardingPosition(tx.to().stopPosition());

    if (!tx.sameStop()) {
      transfer(tx.getPathTransfer(), tx.to().stop());
    }

    // The transfer may happen before the original boarding point. If so, the boarding must be
    // changed so that the leg is valid (not traveling in reverse/back in time). Also, setting
    // the boarding position to the first stop in the pattern makes sure that all paths start at the
    // same place; hence the generalized-cost can be compared.
    // The board position will be changed when a new head is inserted.
    int boardStopPos = 0;

    var trip = originalLeg.trip();
    var times = new BoardAndAlightTime(trip, boardStopPos, tx.from().stopPosition());
    transit(trip, times, tx.constrainedTransfer());

    return this;
  }

  @Override
  public OptimizedPath<T> build() {
    return new OptimizedPath<>(
      createPathLegs(costCalculator(), slackProvider()),
      iterationDepartureTime,
      generalizedCost,
      c2(),
      transferPriorityCost,
      waitTimeOptimizedCost,
      breakTieCost()
    );
  }

  @Override
  public String toString() {
    var builder = ValueObjectToStringBuilder.of().addObj(super.toString()).addText(" [");

    if (generalizedCost != RaptorCostCalculator.ZERO_COST) {
      builder.addObj(RaptorValueFormatter.formatC1(generalizedCost()));
    }
    if (c2() != RaptorConstants.NOT_SET) {
      builder.addObj(RaptorValueFormatter.formatC2(c2()));
    }
    if (transferPriorityCost != TransferConstraint.ZERO_COST) {
      builder.addObj(RaptorValueFormatter.formatTransferPriority(transferPriorityCost));
    }
    if (waitTimeOptimizedCost != TransferWaitTimeCostCalculator.ZERO_COST) {
      builder.addObj(RaptorValueFormatter.formatWaitTimeCost(generalizedCostWaitTimeOptimized()));
    }
    return builder.addText("]").toString();
  }

  @Override
  protected void add(PathBuilderLeg<T> newLeg) {
    addHead(newLeg);
    // Keep from- and to- times up to date by time-shifting access, transfer and egress legs.
    newLeg.timeShiftThisAndNextLeg(slackProvider(), iterationDepartureTime);
    addTransferPriorityCost(newLeg);
    addOptimizedWaitTimeCost(newLeg);
    updateGeneralizedCost();
  }

  @Override
  protected void updateAggregatedFields() {
    /* Empty, aggregated fields are updated while adding new legs */
  }

  /**
   * Return the generalized cost for the current set of paths.
   */
  public int generalizedCost() {
    return generalizedCost;
  }

  /**
   * The latest possible time to board. We use the first transit leg arrival time as the limit, you
   * need to board before you alight.
   */
  public int latestPossibleBoardingTime() {
    return head().toTime();
  }

  @Override
  public int transferPriorityCost() {
    return transferPriorityCost;
  }

  @Override
  public int generalizedCostWaitTimeOptimized() {
    return generalizedCost + waitTimeOptimizedCost;
  }

  @Override
  public int breakTieCost() {
    // We add the arrival times together to mimic doing the transfers as early as possible
    // when more than one transfer point exists between two trips.
    // We calculate this on the fly, because it is not likely to be done very often and
    // the calculation is light-weight.
    return legsAsStream().filter(PathBuilderLeg::isTransit).mapToInt(PathBuilderLeg::toTime).sum();
  }

  private void updateGeneralizedCost() {
    if (skipCostCalc()) {
      return;
    }
    this.generalizedCost = legsAsStream()
      .mapToInt(it -> it.c1(costCalculator(), slackProvider()))
      .sum();
  }

  /*private methods */

  private void addTransferPriorityCost(PathBuilderLeg<T> pathLeg) {
    boolean transferExist = pathLeg.isTransit() && pathLeg.nextTransitLeg() != null;
    this.transferPriorityCost += OptimizedPath.priorityCost(
      transferExist,
      pathLeg::constrainedTransferAfterLeg
    );
  }

  /**
   * Add cost of wait-time, if the given path leg is a transit leg and it is followed by another
   * transit leg (with a optional transfer leg in between).
   * <p>
   * Guaranteed and stay-seated transfers have zero wait-time cost.
   * <p>
   * We could argue that we should include cost for wait-time after FLEX access, and wait-time
   * before FLEX egress. But since it can be time-shifted, it become almost impossible to do a
   * proper cost calculation for it. For example, if the FLEX ride is pre-booked, then it might wait
   * for the passenger.
   */
  private void addOptimizedWaitTimeCost(PathBuilderLeg<?> pathLeg) {
    if (waitTimeCostCalculator == null) {
      return;
    }

    waitTimeOptimizedCost += extraStopPriorityCost(pathLeg);

    if (!pathLeg.isTransit() || pathLeg.nextTransitLeg() == null) {
      return;
    }

    int waitTime = pathLeg.waitTimeBeforeNextTransitIncludingSlack();
    if (waitTime < 0) {
      return;
    }

    var tx = pathLeg.constrainedTransferAfterLeg();

    if (tx != null) {
      var c = (TransferConstraint) tx.getTransferConstraint();
      // If the transfer is stay-seated or guaranteed, then no wait-time cost is added
      if (c != null && c.isFacilitated()) {
        if (c.isStaySeated()) {
          this.waitTimeOptimizedCost += waitTimeCostCalculator.calculateStaySeatedTransferCost();
        } else if (c.isGuaranteed()) {
          this.waitTimeOptimizedCost += waitTimeCostCalculator.calculateGuaranteedTransferCost();
        }
        return;
      }
    }

    this.waitTimeOptimizedCost += waitTimeCostCalculator.calculateOptimizedWaitCost(waitTime);
  }

  private int extraStopPriorityCost(PathBuilderLeg<?> leg) {
    if (stopPriorityCostCalculator == null) {
      return RaptorCostCalculator.ZERO_COST;
    }

    int extraCost = RaptorCostCalculator.ZERO_COST;
    // Ideally we would like to add the board- & alight-stop-cost when a new transit-leg
    // is added to the path. But, the board stop is unknown until the leg before it is
    // added. So, instead of adding the board-stop-cost when it is added, we wait and add it
    // when a new leg is added in front of it.
    if (leg.next() != null && leg.next().isTransit()) {
      extraCost += stopPriorityCostCalculator.extraStopPriorityCost(leg.toStop());
    }
    if (leg.isTransit()) {
      extraCost += stopPriorityCostCalculator.extraStopPriorityCost(leg.toStop());
    }
    return extraCost;
  }
}
