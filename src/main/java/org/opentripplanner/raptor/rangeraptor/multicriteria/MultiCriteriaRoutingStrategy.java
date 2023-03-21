package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * The purpose of this class is to implement the multi-criteria specific functionality of the
 * worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class MultiCriteriaRoutingStrategy<
  T extends RaptorTripSchedule, R extends PatternRide<T>
>
  implements RoutingStrategy<T> {

  private final McRangeRaptorWorkerState<T> state;
  private final TimeBasedBoardingSupport<T> boardingSupport;
  private final PatternRideFactory<T, R> patternRideFactory;
  private final ParetoSet<R> patternRides;
  private final RaptorCostCalculator<T> generalizedCostCalculator;
  private final SlackProvider slackProvider;

  public MultiCriteriaRoutingStrategy(
    McRangeRaptorWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    PatternRideFactory<T, R> patternRideFactory,
    RaptorCostCalculator<T> generalizedCostCalculator,
    SlackProvider slackProvider,
    ParetoSet<R> patternRides
  ) {
    this.state = state;
    this.boardingSupport = boardingSupport;
    this.patternRideFactory = patternRideFactory;
    this.generalizedCostCalculator = generalizedCostCalculator;
    this.slackProvider = slackProvider;
    this.patternRides = patternRides;
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    state.setAccessToStop(accessPath, departureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorRoute<T> route) {
    boardingSupport.prepareForTransitWith(route.timetable());
    patternRideFactory.prepareForTransitWith(route.pattern());
    this.patternRides.clear();
  }

  @Override
  public void alightOnlyRegularTransferExist(int stopIndex, int stopPos, int alightSlack) {
    for (R ride : patternRides) {
      state.transitToStop(ride, stopIndex, ride.trip().arrival(stopPos), alightSlack);
    }
  }

  @Override
  public void alightConstrainedTransferExist(int stopIndex, int stopPos, int alightSlack) {
    // There is no difference in alight with and without constrained transfers.
    // The alight-slack is removed at the next boarding if the constrained transfer apply.
    alightOnlyRegularTransferExist(stopIndex, stopPos, alightSlack);
  }

  @Override
  public void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack) {
    for (McStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
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
    for (McStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
      boardWithConstrainedTransfer(prevArrival, stopIndex, stopPos, boardSlack, txSearch);
    }
  }

  @SuppressWarnings("unchecked")
  private void board(
    McStopArrival<T> prevArrival,
    final int stopIndex,
    final RaptorBoardOrAlightEvent<T> boarding
  ) {
    final T trip = boarding.trip();
    final int boardTime = boarding.time();

    if (prevArrival.arrivedBy(ACCESS)) {
      int latestArrivalTime = boardTime - slackProvider.boardSlack(trip.pattern().slackIndex());
      prevArrival = prevArrival.timeShiftNewArrivalTime(latestArrivalTime);
    }

    final int boardC1 = calculateCostAtBoardTime(prevArrival, boarding);

    final int relativeBoardC1 = boardC1 + calculateOnTripRelativeCost(boardTime, trip);

    R ride = patternRideFactory.createPatternRide(
      prevArrival,
      stopIndex,
      boarding.stopPositionInPattern(),
      boardTime,
      boardC1,
      relativeBoardC1,
      trip
    );
    patternRides.add(ride);
  }

  private void boardWithRegularTransfer(
    McStopArrival<T> prevArrival,
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
    McStopArrival<T> prevArrival,
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
    McStopArrival<T> prevArrival,
    final RaptorBoardOrAlightEvent<T> boardEvent
  ) {
    return (
      prevArrival.c1() +
      generalizedCostCalculator.boardingCost(
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
    return generalizedCostCalculator.onTripRelativeRidingCost(boardTime, tripSchedule);
  }
}
