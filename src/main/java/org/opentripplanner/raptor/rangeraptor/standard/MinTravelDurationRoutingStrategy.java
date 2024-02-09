package org.opentripplanner.raptor.rangeraptor.standard;

import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to implement a routing strategy for finding the best minimum travel
 * duration ignoring wait time(except board-/alight-slack). This class optimize on a single
 * criteria: MINIMUM TRAVEL DURATION.
 * <p>
 * Note! Raptor give us number-of-transfer as a second pareto criteria - which is outside the scope
 * of this class.
 * <p>
 * Note! This strategy should only be used with one Range Raptor iteration (no searchWindow).
 * Multiple iterations are not allowed and would produce the same result in every iteration.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class MinTravelDurationRoutingStrategy<T extends RaptorTripSchedule>
  implements RoutingStrategy<T> {

  private static final Logger LOG = LoggerFactory.getLogger(MinTravelDurationRoutingStrategy.class);

  private static final int NOT_SET = -1;

  private final StdWorkerState<T> state;
  private final TimeBasedBoardingSupport<T> boardingSupport;
  private final TransitCalculator<T> calculator;

  private int logCount = 0;
  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;
  private int onTripTimeShift;
  private int iterationDepartureTime;

  public MinTravelDurationRoutingStrategy(
    StdWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    TransitCalculator<T> calculator,
    WorkerLifeCycle lifeCycle
  ) {
    this.state = state;
    this.boardingSupport = boardingSupport;
    this.calculator = calculator;
    lifeCycle.onSetupIteration(it -> this.iterationDepartureTime = it);
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    state.setAccessToStop(accessPath, iterationDepartureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorRoute<T> route) {
    this.boardingSupport.prepareForTransitWith(route.timetable());
    this.onTripIndex = UNBOUNDED_TRIP_INDEX;
    this.onTripBoardTime = NOT_SET;
    this.onTripBoardStop = NOT_SET;
    this.onTrip = null;
    this.onTripTimeShift = NOT_SET;
  }

  @Override
  public void alightOnlyRegularTransferExist(int stopIndex, int stopPos, int alightSlack) {
    alight(stopIndex, stopPos, alightSlack);
  }

  @Override
  public void alightConstrainedTransferExist(int stopIndex, int stopPos, int alightSlack) {
    // Because we do not know if a constrained transfer can be used or not we need to
    // assume there is a guaranteed transfer; Hence zero alightSlack
    alight(stopIndex, stopPos, 0);
  }

  @Override
  public void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack) {
    int prevArrivalTime = prevArrivalTime(stopIndex);
    boardingSupport
      .searchRegularTransfer(prevArrivalTime, stopPos, boardSlack, onTripIndex)
      .boardWithFallback(
        boarding -> board(stopIndex, boarding),
        emptyBoarding -> boardSameTrip(emptyBoarding.earliestBoardTime(), stopPos, stopIndex)
      );
  }

  @Override
  public void boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  ) {
    // If a constrained transfer exist we drop the board slack since it potentially
    // could be a guaranteed or stay-seated transfers. We can not check the two trips and
    // base our decision on that, because the optimal trips is unknown.
    boardWithRegularTransfer(stopIndex, stopPos, 0);
  }

  private void alight(int stopIndex, int stopPos, int alightSlackApplied) {
    // attempt to alight if we're on board
    if (onTripIndex != UNBOUNDED_TRIP_INDEX) {
      // Trip alightTime + alight-slack(forward-search) or board-slack(reverse-search)
      final int stopArrivalTime0 = calculator.stopArrivalTime(onTrip, stopPos, alightSlackApplied);

      // Remove the wait time from the arrival-time. We don´t need to use the transit
      // calculator because of the way we compute the time-shift. It is positive in the case
      // of a forward-search and negative in the case of a reverse-search.
      final int stopArrivalTime = stopArrivalTime0 - onTripTimeShift;

      // TODO: Make sure that the TimeTables can not have negative trip times, then
      //       this check can be removed.
      if (calculator.isBefore(stopArrivalTime, onTripBoardTime)) {
        logInvalidAlightTime(stopPos, stopArrivalTime);
      } else {
        state.transitToStop(stopIndex, stopArrivalTime, onTripBoardStop, onTripBoardTime, onTrip);
      }
    }
  }

  private void board(int stopIndex, RaptorBoardOrAlightEvent<T> boarding) {
    onTripIndex = boarding.tripIndex();
    onTrip = boarding.trip();
    onTripBoardTime = boarding.earliestBoardTime();
    onTripBoardStop = stopIndex;
    // Calculate the time-shift, the time-shift will be a positive duration in a
    // forward-search, and a negative value in case of a reverse-search.
    onTripTimeShift = boarding.time() - onTripBoardTime;
  }

  /**
   * This method allow the strategy to replace the existing boarding (if it exists) with a better
   * option.
   *
   * @param earliestBoardTime - the earliest possible time a boarding can take place
   * @param stopPos           - the pattern stop position
   * @param stopIndex         - the global stop index
   */
  private void boardSameTrip(int earliestBoardTime, int stopPos, int stopIndex) {
    // If not boarded, return
    if (onTripIndex == UNBOUNDED_TRIP_INDEX) {
      return;
    }

    int tripBoardingTime = onTrip.departure(stopPos);

    // Return if the current boarding time is after the earliest boarding time
    if (calculator.isAfter(earliestBoardTime, tripBoardingTime)) {
      return;
    }

    int tripTimeShift = tripBoardingTime - earliestBoardTime;

    // Return if the previous boarding time is better than this (the previous boarding can be
    // time-shifted more than this)
    if (calculator.isBefore(tripTimeShift, onTripTimeShift)) {
      return;
    }

    onTripBoardTime = earliestBoardTime;
    onTripBoardStop = stopIndex;
    onTripTimeShift = tripTimeShift;
  }

  private int prevArrivalTime(int stopIndex) {
    return state.bestTimePreviousRound(stopIndex);
  }

  private void logInvalidAlightTime(int stopPos, int stopArrivalTime) {
    if (logCount < 3) {
      ++logCount;
      LOG.error(
        "Traveling back in time is not allowed. Board time: {}, alight stop pos: {}, stop arrival time: {}, trip: {}.",
        TimeUtils.timeToStrLong(onTripBoardTime),
        stopPos,
        TimeUtils.timeToStrLong(stopArrivalTime),
        onTrip
      );
    }
  }
}
