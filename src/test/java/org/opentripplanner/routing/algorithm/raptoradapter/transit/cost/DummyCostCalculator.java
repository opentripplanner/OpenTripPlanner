package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class DummyCostCalculator implements CostCalculator {

  public static final int COST = 99;

  @Override
  public int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStop,
    int boardTime,
    RaptorTripSchedule trip,
    RaptorTransferConstraint transferConstraints
  ) {
    return COST;
  }

  @Override
  public int onTripRelativeRidingCost(int boardTime, int transitFactorIndex) {
    return COST;
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitTime,
    int transitFactorIndex,
    int toStop
  ) {
    return COST;
  }

  @Override
  public int waitCost(int waitTimeInSeconds) {
    return COST;
  }

  @Override
  public int calculateMinCost(int minTravelTime, int minNumTransfers) {
    return COST;
  }

  @Override
  public int costEgress(RaptorTransfer egress) {
    return COST;
  }
}
