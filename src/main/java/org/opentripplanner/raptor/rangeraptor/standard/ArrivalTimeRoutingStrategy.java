package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.function.IntConsumer;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.TransitArrival;

/**
 * The purpose of this class is to implement a routing strategy for finding the best arrival-time.
 * This class optimize the raptor search on a single criteria.
 * <p>
 * Note! Raptor give us number-of-transfer as a second pareto criteria - which is outside the scope
 * of this class.
 * <p>
 * Note! This strategy can be used with RangeRaptor - iterating over a time-window to get pareto
 * optimal solution for departure time. Which is outside the scope of this class.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class ArrivalTimeRoutingStrategy<T extends RaptorTripSchedule>
  implements RoutingStrategy<T> {

  private static final int NOT_SET = -1;

  private final TransitCalculator<T> calculator;
  private final StdWorkerState<T> state;

  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;

  public ArrivalTimeRoutingStrategy(TransitCalculator<T> calculator, StdWorkerState<T> state) {
    this.calculator = calculator;
    this.state = state;
  }

  @Override
  public void setAccessToStop(
    RaptorAccessEgress accessPath,
    int iterationDepartureTime,
    int timeDependentDepartureTime
  ) {
    state.setAccessToStop(accessPath, timeDependentDepartureTime);
  }

  @Override
  public void prepareForTransitWith() {
    this.onTripIndex = NOT_SET;
    this.onTripBoardTime = NOT_SET;
    this.onTripBoardStop = NOT_SET;
    this.onTrip = null;
  }

  @Override
  public void alight(final int stopIndex, final int stopPos, final int alightSlack) {
    if (onTripIndex != NOT_SET) {
      final int stopArrivalTime = calculator.stopArrivalTime(onTrip, stopPos, alightSlack);
      state.transitToStop(stopIndex, stopArrivalTime, onTripBoardStop, onTripBoardTime, onTrip);
    }
  }

  @Override
  public void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer) {
    // Don't attempt to board if this stop was not reached in the last round.
    // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
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
    int stopIndex,
    final int earliestBoardTime,
    RaptorTripScheduleBoardOrAlightEvent<T> boarding
  ) {
    onTripIndex = boarding.getTripIndex();
    onTrip = boarding.getTrip();
    onTripBoardTime = boarding.getTime();
    onTripBoardStop = stopIndex;
  }

  @Override
  public int onTripIndex() {
    return onTripIndex;
  }
}
