package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.function.IntConsumer;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
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

  private final TransitCalculator<T> calculator;
  private final StdWorkerState<T> state;

  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;
  private int onTripTimeShift;

  public MinTravelDurationRoutingStrategy(
    TransitCalculator<T> calculator,
    StdWorkerState<T> state
  ) {
    this.calculator = calculator;
    this.state = state;
  }

  @Override
  public void setAccessToStop(
    RaptorAccessEgress accessPath,
    int iterationDepartureTime,
    int timeDependentDepartureTime
  ) {
    // Pass in the original departure time, as wait time should not be included
    state.setAccessToStop(accessPath, iterationDepartureTime);
  }

  @Override
  public void prepareForTransitWith() {
    this.onTripIndex = NOT_SET;
    this.onTripBoardTime = NOT_SET;
    this.onTripBoardStop = NOT_SET;
    this.onTrip = null;
    this.onTripTimeShift = NOT_SET;
  }

  @Override
  public void alight(int stopIndex, int stopPos, int alightSlack) {
    // attempt to alight if we're on board
    if (onTripIndex != NOT_SET) {
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
  public void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer) {
    if (state.isStopReachedInPreviousRound(stopIndex)) {
      prevStopArrivalTimeConsumer.accept(state.bestTimePreviousRound(stopIndex));
    }
  }

  @Override
  public TransitArrival<T> previousTransit(int boardStopIndex) {
    return state.previousTransit(boardStopIndex);
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
  public int onTripIndex() {
    return onTripIndex;
  }

  @Override
  public void boardSameTrip(int earliestBoardTime, int stopPos, int stopIndex) {
    // If not boarded, return
    if (onTripIndex == NOT_SET) {
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
}
