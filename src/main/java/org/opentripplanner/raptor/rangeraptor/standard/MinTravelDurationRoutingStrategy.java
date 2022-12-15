package org.opentripplanner.raptor.rangeraptor.standard;

import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedRoutingSupport;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedRoutingSupportCallback;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.TransitArrival;

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
  implements RoutingStrategy<T>, TimeBasedRoutingSupportCallback<T> {

  private static final int NOT_SET = -1;

  private final StdWorkerState<T> state;
  private final TimeBasedRoutingSupport<T> routingSupport;
  private final TransitCalculator<T> calculator;

  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;
  private int onTripTimeShift;

  public MinTravelDurationRoutingStrategy(
    StdWorkerState<T> state,
    SlackProvider slackProvider,
    TransitCalculator<T> calculator,
    RoundProvider roundProvider,
    WorkerLifeCycle lifecycle
  ) {
    this.state = state;
    this.routingSupport =
      new TimeBasedRoutingSupport<>(slackProvider, calculator, roundProvider, lifecycle);
    this.routingSupport.withCallback(this);
    this.calculator = calculator;
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    state.setAccessToStop(accessPath, iterationDepartureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    this.routingSupport.prepareForTransitWith(timeTable);
    this.onTripIndex = UNBOUNDED_TRIP_INDEX;
    this.onTripBoardTime = NOT_SET;
    this.onTripBoardStop = NOT_SET;
    this.onTrip = null;
    this.onTripTimeShift = NOT_SET;
  }

  @Override
  public void alight(int stopIndex, int stopPos, int alightSlack) {
    // attempt to alight if we're on board
    if (onTripIndex != UNBOUNDED_TRIP_INDEX) {
      // Trip alightTime + alight-slack(forward-search) or board-slack(reverse-search)
      final int stopArrivalTime0 = calculator.stopArrivalTime(onTrip, stopPos, alightSlack);

      // Remove the wait time from the arrival-time. We don´t need to use the transit
      // calculator because of the way we compute the time-shift. It is positive in the case
      // of a forward-search and negative int he case of a reverse-search.
      final int stopArrivalTime = stopArrivalTime0 - onTripTimeShift;

      state.transitToStop(stopIndex, stopArrivalTime, onTripBoardStop, onTripBoardTime, onTrip);
    }
  }

  @Override
  public void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack) {
    int prevArrivalTime = prevArrivalTime(stopIndex);
    routingSupport.boardWithRegularTransfer(
      prevArrivalTime,
      stopIndex,
      stopPos,
      boardSlack,
      onTripIndex
    );
  }

  @Override
  public boolean boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedTripScheduleBoardingSearch<T> txSearch
  ) {
    return routingSupport.boardWithConstrainedTransfer(
      previousTransitArrival(stopIndex),
      prevArrivalTime(stopIndex),
      stopIndex,
      boardSlack,
      txSearch
    );
  }

  @Override
  public void board(
    final int stopIndex,
    final int earliestBoardTime,
    final RaptorTripScheduleBoardOrAlightEvent<T> boarding
  ) {
    onTripIndex = boarding.getTripIndex();
    onTrip = boarding.getTrip();
    onTripBoardTime = earliestBoardTime;
    onTripBoardStop = stopIndex;
    // Calculate the time-shift, the time-shift will be a positive duration in a
    // forward-search, and a negative value in case of a reverse-search.
    onTripTimeShift = boarding.getTime() - onTripBoardTime;
  }

  @Override
  public void boardSameTrip(int earliestBoardTime, int stopPos, int stopIndex) {
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

  private TransitArrival<T> previousTransitArrival(int boardStopIndex) {
    return state.previousTransit(boardStopIndex);
  }
}
