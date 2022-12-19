package org.opentripplanner.raptor.rangeraptor.standard;

import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedRoutingSupport;
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
  implements RoutingStrategy<T> {

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
    TimeBasedRoutingSupport<T> routingSupport,
    TransitCalculator<T> calculator
  ) {
    this.state = state;
    this.routingSupport = routingSupport;
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
    var boarding = routingSupport.boardWithRegularTransfer(
      prevArrivalTime(stopIndex),
      stopPos,
      boardSlack,
      onTripIndex
    );
    if (boarding.empty()) {
      boardSameTrip(boarding.getEarliestBoardTime(), stopPos, stopIndex);
    } else {
      board(stopIndex, boarding);
    }
  }

  @Override
  public void boardWithConstrainedTransfer(
    final int stopIndex,
    final int stopPos,
    final int boardSlack,
    RaptorConstrainedTripScheduleBoardingSearch<T> txSearch
  ) {
    var boarding = routingSupport.boardWithConstrainedTransfer(
      previousTransitArrival(stopIndex),
      prevArrivalTime(stopIndex),
      boardSlack,
      txSearch
    );

    if (boarding.empty()) {
      boardWithRegularTransfer(stopIndex, stopPos, boardSlack);
    } else if (!boarding.getTransferConstraint().isNotAllowed()) {
      board(stopIndex, boarding);
    }
  }

  public void board(final int stopIndex, final RaptorTripScheduleBoardOrAlightEvent<T> boarding) {
    onTripIndex = boarding.getTripIndex();
    onTrip = boarding.getTrip();
    onTripBoardTime = boarding.getEarliestBoardTime();
    onTripBoardStop = stopIndex;
    // Calculate the time-shift, the time-shift will be a positive duration in a
    // forward-search, and a negative value in case of a reverse-search.
    onTripTimeShift = boarding.getTime() - onTripBoardTime;
  }

  /**
   * This method allow the strategy to replace the existing boarding (if it exists) with a better
   * option. It is left to the implementation to check that a boarding already exist.
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

  private TransitArrival<T> previousTransitArrival(int boardStopIndex) {
    return state.previousTransit(boardStopIndex);
  }
}
