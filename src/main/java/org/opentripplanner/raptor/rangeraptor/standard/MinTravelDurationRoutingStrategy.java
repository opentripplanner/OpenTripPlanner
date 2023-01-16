package org.opentripplanner.raptor.rangeraptor.standard;

import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;

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
  private final TimeBasedBoardingSupport<T> boardingSupport;
  private final TransitCalculator<T> calculator;

  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;
  private int onTripTimeShift;

  public MinTravelDurationRoutingStrategy(
    StdWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    TransitCalculator<T> calculator
  ) {
    this.state = state;
    this.boardingSupport = boardingSupport;
    this.calculator = calculator;
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    state.setAccessToStop(accessPath, iterationDepartureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    this.boardingSupport.prepareForTransitWith(timeTable);
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
    boardingSupport
      .searchRegularTransfer(prevArrivalTime(stopIndex), stopPos, boardSlack, onTripIndex)
      .boardWithFallback(
        boarding -> board(stopIndex, boarding),
        emptyBoarding -> boardSameTrip(emptyBoarding.earliestBoardTime(), stopPos, stopIndex)
      );
  }

  @Override
  public void boardWithConstrainedTransfer(
    final int stopIndex,
    final int stopPos,
    final int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  ) {
    boardingSupport
      .searchConstrainedTransfer(
        previousTransitArrival(stopIndex),
        prevArrivalTime(stopIndex),
        boardSlack,
        txSearch
      )
      .boardWithFallback(
        boarding -> board(stopIndex, boarding),
        emptyBoarding -> boardWithRegularTransfer(stopIndex, stopPos, boardSlack)
      );
  }

  public void board(final int stopIndex, final RaptorBoardOrAlightEvent<T> boarding) {
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
