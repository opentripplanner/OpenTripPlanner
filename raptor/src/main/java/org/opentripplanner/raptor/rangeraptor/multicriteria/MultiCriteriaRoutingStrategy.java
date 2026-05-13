package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.view.PathLegType.ACCESS;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorStartOnBoardAccess;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.OnTripAccessArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.AbstractPatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * The purpose of this class is to implement the multi-criteria specific functionality of the
 * worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class MultiCriteriaRoutingStrategy<
  T extends RaptorTripSchedule,
  R extends AbstractPatternRide<T>
>
  implements RoutingStrategy<T> {

  private final McRangeRaptorWorkerState<T> state;
  private final TimeBasedBoardingSupport<T> boardingSupport;
  private final PatternRideFactory<T, R> patternRideFactory;
  private final ParetoSet<R> patternRides;
  private final RaptorCostCalculator<T> c1Calculator;
  private final SlackProvider slackProvider;

  public MultiCriteriaRoutingStrategy(
    McRangeRaptorWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    PatternRideFactory<T, R> patternRideFactory,
    RaptorCostCalculator<T> c1Calculator,
    SlackProvider slackProvider,
    ParetoSet<R> patternRides
  ) {
    this.state = Objects.requireNonNull(state);
    this.boardingSupport = Objects.requireNonNull(boardingSupport);
    this.patternRideFactory = Objects.requireNonNull(patternRideFactory);
    this.c1Calculator = Objects.requireNonNull(c1Calculator);
    this.slackProvider = Objects.requireNonNull(slackProvider);
    this.patternRides = Objects.requireNonNull(patternRides);
  }

  @Override
  public void addAccessStopArrival(RaptorAccessEgress accessPath, int departureTime) {
    state.addAccessToStop(accessPath, departureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorRoute<T> route) {
    boardingSupport.prepareForTransitWith(route.timetable());
    patternRideFactory.prepareForTransitWith(route.pattern());
    this.patternRides.clear();
  }

  @Override
  public void alightOnlyRegularTransferExist(int stopIndex, int stopPos, int alightSlack) {
    for (int i = 0; i < patternRides.size(); ++i) {
      R ride = patternRides.get(i);
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

  @Override
  public void addStartOnBoardAccessStopArrival(RaptorStartOnBoardAccess access, int boardTime) {
    state.addOnTripAccessStopArrival(access, boardTime);
  }

  @Nullable
  @Override
  public OnTripAccessArrivals<T> consumeStartOnBoardStopArrivalsForRoute(int routeIndex) {
    return state.consumeOnTripStopArrivalsForRoute(routeIndex);
  }

  @Override
  public void boardWithStartOnBoardAccess(
    ArrivalView<T> prevArrival,
    T trip,
    int stopPositionInPattern
  ) {
    if (!(prevArrival instanceof McStopArrival<T> prevMcArrival)) {
      throw new IllegalArgumentException(prevArrival.toString());
    }

    // Use the trip's exact departure time to find the same trip that was riding in the previous segment
    var boarding = boardingSupport.searchForRegularBoarding(
      trip.departure(stopPositionInPattern),
      stopPositionInPattern,
      RaptorConstants.ZERO
    );
    if (boarding.empty()) {
      throw new IllegalArgumentException("Unable to board trip " + trip + " from " + prevArrival);
    }
    board(prevMcArrival, prevArrival.stop(), boarding);
  }

  private void boardWithRegularTransfer(
    McStopArrival<T> prevArrival,
    int stopIndex,
    int stopPos,
    int boardSlack
  ) {
    var result = boardingSupport.searchForRegularBoarding(
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
    var boarding = boardingSupport.searchForConstrainedBoarding(
      prevArrival.mostRecentTransitArrival(),
      prevArrival.arrivalTime(),
      boardSlack,
      txSearch
    );
    if (boarding.empty()) {
      boardWithRegularTransfer(prevArrival, stopIndex, stopPos, boardSlack);
    } else if (!boarding.transferConstraint().isNotAllowed()) {
      board(prevArrival, stopIndex, boarding);
    }
  }

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

    var ride = patternRideFactory.createPatternRide(
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
      c1Calculator.boardingCost(
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
   * Calculate a cost for riding a trip. The cost is used to compare trips boarding in the same
   * pattern with the same number of transfers. It is ok for the cost to be relative to any
   * point in place or time - as long as it can be used to compare to paths that started at the
   * origin in the same iteration, having used the same number-of-rounds to board trips in the same
   * pattern.
   */
  private int calculateOnTripRelativeCost(int boardTime, T tripSchedule) {
    return c1Calculator.transitCost(tripSchedule.relativeTravelDuration(boardTime), tripSchedule);
  }
}
