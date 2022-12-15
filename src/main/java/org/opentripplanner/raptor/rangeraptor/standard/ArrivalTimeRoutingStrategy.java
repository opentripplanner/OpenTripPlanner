package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.function.IntConsumer;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedRoutingSupport;
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
  extends TimeBasedRoutingSupport<T> {

  private static final int NOT_SET = -1;

  private final StdWorkerState<T> state;

  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;

  public ArrivalTimeRoutingStrategy(
    StdWorkerState<T> state,
    TransitCalculator<T> calculator,
    SlackProvider slackProvider,
    RoundProvider roundProvider,
    WorkerLifeCycle lifecycle
  ) {
    super(slackProvider, calculator, roundProvider, lifecycle);
    this.state = state;
  }

  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    int departureTime = getTimeDependentDepartureTime(accessPath, iterationDepartureTime);

    // This access is not available after the iteration departure time
    if (departureTime == -1) {
      return;
    }

    state.setAccessToStop(accessPath, departureTime);
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
    prevStopArrivalTimeConsumer.accept(state.bestTimePreviousRound(stopIndex));
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
