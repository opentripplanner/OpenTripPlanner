package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;
import static org.opentripplanner.raptor.rangeraptor.multicriteria.PatternRide.paretoComparatorRelativeCost;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * The purpose of this class is to implement the multi-criteria specific functionality of the
 * worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class MultiCriteriaRoutingStrategy<T extends RaptorTripSchedule>
  implements RoutingStrategy<T> {

  private final McRangeRaptorWorkerState<T> state;
  private final TimeBasedBoardingSupport<T> boardingSupport;
  private final ParetoSet<PatternRide<T>> patternRides;
  private final TransitCalculator<T> calculator;
  private final CostCalculator<T> costCalculator;
  private final SlackProvider slackProvider;

  public MultiCriteriaRoutingStrategy(
    McRangeRaptorWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    TransitCalculator<T> calculator,
    CostCalculator<T> costCalculator,
    SlackProvider slackProvider,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    this.state = state;
    this.boardingSupport = boardingSupport;
    this.calculator = calculator;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.patternRides =
      new ParetoSet<>(
        paretoComparatorRelativeCost(),
        debugHandlerFactory.paretoSetPatternRideListener()
      );
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    state.setAccessToStop(accessPath, departureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    boardingSupport.prepareForTransitWith(timeTable);
    this.patternRides.clear();
  }

  @Override
  public void alight(final int stopIndex, final int stopPos, int alightSlack) {
    for (PatternRide<T> ride : patternRides) {
      state.transitToStop(ride, stopIndex, ride.trip().arrival(stopPos), alightSlack);
    }
  }

  @Override
  public void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack) {
    for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
      boardWithRegularTransfer(prevArrival, stopIndex, stopPos, boardSlack);
    }
  }

  @Override
  public void boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  ) {
    for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
      boardWithConstrainedTransfer(prevArrival, stopIndex, stopPos, boardSlack, txSearch);
    }
  }

  private void board(
    AbstractStopArrival<T> prevArrival,
    final int stopIndex,
    final RaptorBoardOrAlightEvent<T> boarding
  ) {
    final T trip = boarding.trip();
    final int boardTime = boarding.time();

    if (prevArrival.arrivedBy(ACCESS)) {
      int latestArrivalTime = boardTime - slackProvider.boardSlack(trip.pattern().slackIndex());
      prevArrival = prevArrival.timeShiftNewArrivalTime(latestArrivalTime);
    }

    final int boardCost = calculateCostAtBoardTime(prevArrival, boarding);

    final int relativeBoardCost = boardCost + calculateOnTripRelativeCost(boardTime, trip);

    patternRides.add(
      new PatternRide<>(
        prevArrival,
        stopIndex,
        boarding.stopPositionInPattern(),
        boardTime,
        boardCost,
        relativeBoardCost,
        trip
      )
    );
  }

  private void boardWithRegularTransfer(
    AbstractStopArrival<T> prevArrival,
    int stopIndex,
    int stopPos,
    int boardSlack
  ) {
    var result = boardingSupport.searchRegularTransfer(
      prevArrival.arrivalTime(),
      stopPos,
      boardSlack
    );
    if (!result.empty()) {
      board(prevArrival, stopIndex, result);
    }
  }

  private void boardWithConstrainedTransfer(
    AbstractStopArrival<T> prevArrival,
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  ) {
    boardingSupport
      .searchConstrainedTransfer(
        prevArrival.mostRecentTransitArrival(),
        prevArrival.arrivalTime(),
        boardSlack,
        txSearch
      )
      .boardWithFallback(
        boarding -> board(prevArrival, stopIndex, boarding),
        emptyBoarding -> boardWithRegularTransfer(prevArrival, stopIndex, stopPos, boardSlack)
      );
  }

  /**
   * Calculate a cost for riding a trip. It should include the cost from the beginning of the
   * journey all the way until a trip is boarded. Any slack at the end of the last leg is not part
   * of this, because that is already accounted for. If the previous leg is an access leg, then it
   * is already time-shifted, which is important for this calculation to be correct.
   * <p>
   * Note! This depends on the {@code prevArrival} being set.
   */
  private int calculateCostAtBoardTime(
    AbstractStopArrival<T> prevArrival,
    final RaptorBoardOrAlightEvent<T> boardEvent
  ) {
    return (
      prevArrival.cost() +
      costCalculator.boardingCost(
        prevArrival.isFirstRound(),
        prevArrival.arrivalTime(),
        boardEvent.boardStopIndex(),
        boardEvent.time(),
        boardEvent.trip(),
        boardEvent.transferConstraint()
      )
    );
  }

  /**
   * Calculate a cost for riding a trip. It should include the cost from the beginning of the
   * journey all the way until a trip is boarded. The cost is used to compare trips boarding the
   * same pattern with the same number of transfers. It is ok for the cost to be relative to any
   * point in place or time - as long as it can be used to compare to paths that started at the
   * origin in the same iteration, having used the same number-of-rounds to board the same trip.
   */
  private int calculateOnTripRelativeCost(int boardTime, T tripSchedule) {
    return costCalculator.onTripRelativeRidingCost(boardTime, tripSchedule);
  }
}
