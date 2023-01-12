package org.opentripplanner.raptor.heuristic;

import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.standard.StdWorkerState;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Routing strategy for heuristic searches. Does not operate on actual times, but only durations.
 * Does not do any trip searches, but operates on a single heuristic trip.
 */
public class HeuristicRoutingStrategy<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

  public static final int UNREACHED = 999_999_999;

  private final TimeAndCostHeuristicState<T> state;
  private final CostCalculator<T> costCalculator;
  private final TransitCalculator<T> calculator;
  private final RoundProvider roundProvider;
  private T currentTrip;
  private int currentBoardingTime;
  private int currentBoardCost;
  private int currentBoardingTotalDuration;

  public HeuristicRoutingStrategy(
    StdWorkerState<T> state,
    RoundProvider roundProvider,
    TransitCalculator<T> calculator,
    CostCalculator<T> costCalculator
  ) {
    this.state = (TimeAndCostHeuristicState<T>) state;
    this.costCalculator = costCalculator;
    this.calculator = calculator;
    this.roundProvider = roundProvider;
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    state.setAccessToStop(accessPath, iterationDepartureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorTimeTable<T> timetable) {
    this.currentTrip = (T) timetable.getHeuristicTrip();
    this.currentBoardingTime = UNREACHED;
    this.currentBoardingTotalDuration = UNREACHED;
    this.currentBoardCost = UNREACHED;
  }

  @Override
  public void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack) {
    int arrivalDuration = state.bestTimePreviousRound(stopIndex);
    int boardingDuration = arrivalDuration + boardSlack;
    int boardingCost = costCalculator.boardingCost(
      roundProvider.round() < 2,
      arrivalDuration,
      stopIndex,
      boardingDuration,
      currentTrip,
      RaptorTransferConstraint.REGULAR_TRANSFER
    );

    board(stopIndex, stopPos, boardingDuration, boardingCost);
  }

  @Override
  public void boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  ) {
    // Assume 0 boarding slack and cost as it is the lower bound for constrained transfers
    board(stopIndex, stopPos, state.bestTimePreviousRound(stopIndex), 0);
  }

  private void board(int stopIndex, int stopPos, int boardingDuration, int boardCost) {
    int boardingTime = calculator.stopDepartureTime(currentTrip, stopPos);

    if (
      currentBoardingTotalDuration == UNREACHED ||
      (currentBoardingTotalDuration - boardingDuration) >
      calculator.duration(boardingTime, currentBoardingTime)
    ) {
      currentBoardingTime = boardingTime;
      currentBoardingTotalDuration = boardingDuration;

      //  TODO, we should add cost + time to an array, so we can check both what is the minimum time
      //  and cost separately at arrival, now we optimize for minimum time
      currentBoardCost = state.bestCostPreviousRound(stopIndex) + boardCost;
    }
  }

  @Override
  public void alight(int stopIndex, int stopPos, int alightSlack) {
    if (currentBoardingTime != UNREACHED) {
      int transitDuration = calculator.duration(
        currentBoardingTime,
        calculator.stopArrivalTime(currentTrip, stopPos)
      );
      int transitCost = costCalculator.transitArrivalCost(
        currentBoardCost,
        alightSlack,
        transitDuration,
        currentTrip,
        stopIndex
      );
      state.updateNewBestTimeCostAndRound(
        stopIndex,
        currentBoardingTotalDuration + alightSlack + transitDuration,
        currentBoardCost + transitCost,
        true
      );
    }
  }
}
