package org.opentripplanner.raptor._data.transit;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <p/>
 * This class is immutable and thread safe.
 */
public final class TestCostCalculator implements RaptorCostCalculator<TestTripSchedule> {

  private static final int TRANSIT_RELUCTANCE = RaptorCostConverter.toRaptorCost(1);

  private final int boardCost;
  private final int transferCost;
  private final int waitFactor;

  /**
   * Costs for boarding and alighting at a given stop during transfer.
   * See RaptorTransitData.getStopBoardAlightTransferCosts()
   */
  @Nullable
  private final int[] stopBoardAlightTransferCosts;

  /**
   * Cost unit: SECONDS - The unit for all input parameters are in the OTP TRANSIT model cost unit
   * (in Raptor the unit for cost is centi-seconds).
   *
   * @param stopBoardAlightTransferCosts Unit centi-seconds. This parameter is used "as-is" and not
   *                      transformed into the Raptor cast unit to avoid the transformation for each
   *                      request. Use {@code null} to ignore stop cost.
   */
  public TestCostCalculator(
    int boardCost,
    int transferCost,
    double waitReluctanceFactor,
    @Nullable int[] stopBoardAlightTransferCosts
  ) {
    this.boardCost = RaptorCostConverter.toRaptorCost(boardCost);
    this.transferCost = RaptorCostConverter.toRaptorCost(transferCost);
    this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);
    this.stopBoardAlightTransferCosts = stopBoardAlightTransferCosts;
  }

  @Override
  public int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStop,
    int boardTime,
    TestTripSchedule trip,
    RaptorTransferConstraint transferConstraints
  ) {
    if (transferConstraints.isRegularTransfer()) {
      return boardingCostRegularTransfer(firstBoarding, prevArrivalTime, boardStop, boardTime);
    } else {
      return boardingCostConstrainedTransfer(
        prevArrivalTime,
        boardStop,
        boardTime,
        firstBoarding,
        transferConstraints
      );
    }
  }

  @Override
  public int onTripRelativeRidingCost(int boardTime, TestTripSchedule tripScheduledBoarded) {
    // The relative-transit-time is time spent on transit. We do not know the alight-stop, so
    // it is impossible to calculate the "correct" time. But the only thing that maters is that
    // the relative difference between to boardings are correct, assuming riding the same trip.
    // So, we can use the negative board time as relative-transit-time.
    return -boardTime * TRANSIT_RELUCTANCE;
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitTime,
    TestTripSchedule trip,
    int toStop
  ) {
    int cost = boardCost + TRANSIT_RELUCTANCE * transitTime + waitFactor * alightSlack;

    // Add transfer cost on all alighting events.
    // If it turns out to be the last one this cost will be removed during costEgress phase.
    if (stopBoardAlightTransferCosts != null) {
      cost += stopBoardAlightTransferCosts[toStop];
    }

    return cost;
  }

  @Override
  public int waitCost(int waitTimeInSeconds) {
    return waitFactor * waitTimeInSeconds;
  }

  @Override
  public int calculateRemainingMinCost(int minTravelTime, int minNumTransfers, int fromStop) {
    if (minNumTransfers > -1) {
      return (
        boardCost +
        ((boardCost + transferCost) * minNumTransfers) +
        (TRANSIT_RELUCTANCE * minTravelTime)
      );
    } else {
      // Remove cost that was added during alighting similar as we do in the costEgress() method
      return stopBoardAlightTransferCosts == null
        ? (TRANSIT_RELUCTANCE * minTravelTime)
        : (TRANSIT_RELUCTANCE * minTravelTime) - stopBoardAlightTransferCosts[fromStop];
    }
  }

  @Override
  public int costEgress(RaptorAccessEgress egress) {
    if (egress.hasRides()) {
      return egress.c1() + transferCost;
    } else if (stopBoardAlightTransferCosts != null) {
      // Remove cost that was added during alighting.
      // We do not want to add this cost on last alighting since it should only be applied on transfers
      // It has to be done here because during alighting we do not know yet if it will be
      // a transfer or not.
      return egress.c1() - stopBoardAlightTransferCosts[egress.stop()];
    } else {
      return egress.c1();
    }
  }

  /** This is public for test purposes only */
  public int boardingCostRegularTransfer(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStop,
    int boardTime
  ) {
    // Calculate the wait-time before the boarding which should be accounted for in the cost
    // calculation. Any slack at the end of the last leg is not part of this, because it is
    // already accounted for. If the previous leg is an access leg, then it is already
    // time-shifted, which is important for this calculation to be correct.
    final int boardWaitTime = boardTime - prevArrivalTime;

    int cost = waitFactor * boardWaitTime;

    cost += firstBoarding ? boardCost : (boardCost + transferCost);

    // If it's first boarding event then it is not a transfer
    if (stopBoardAlightTransferCosts != null && !firstBoarding) {
      cost += stopBoardAlightTransferCosts[boardStop];
    }
    return cost;
  }

  /* private methods */

  private int boardingCostConstrainedTransfer(
    int prevArrivalTime,
    int boardStop,
    int boardTime,
    boolean firstBoarding,
    RaptorTransferConstraint txConstraints
  ) {
    // This cast could be avoided, if we added another generic type to the Raptor component,
    // but it would be rather messy, just to avoid a single cast.
    var tx = (TestTransferConstraint) txConstraints;

    if (tx.isStaySeated()) {
      final int boardWaitTime = boardTime - prevArrivalTime;
      // For a stay-seated transfer the wait-time is spent on-board and we should use the
      // transitReluctance, not the waitReluctance, to find the cost of the time since
      // the stop arrival. So we take the time and multiply it with the transit reluctance.
      //
      // Note! if the boarding happens BEFORE the previous stop arrival, we will get a
      // negative time - this is ok, so we allow it in this calculation.
      //
      // The previous stop arrival might have a small alight-slack, this should be replaced
      // with "on-board" time, but the slack should be short and the differance between
      // transit reluctance and wait reluctance is also small, so we ignore this.
      //
      return TRANSIT_RELUCTANCE * boardWaitTime;
    } else if (tx.isGuaranteed()) {
      // For a guaranteed transfer we skip board- and transfer-cost
      final int boardWaitTime = boardTime - prevArrivalTime;

      // StopBoardAlightTransferCost is NOT added to the cost here. This is because a trip-to-trip
      // constrained transfer take precedence over stop-to-stop transfer priority (NeTEx station
      // transfer priority).
      return waitFactor * boardWaitTime;
    }

    // fallback to regular transfer
    return boardingCostRegularTransfer(firstBoarding, prevArrivalTime, boardStop, boardTime);
  }
}
