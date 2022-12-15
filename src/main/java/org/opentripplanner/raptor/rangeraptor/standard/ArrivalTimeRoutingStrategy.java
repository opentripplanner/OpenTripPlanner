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
  implements RoutingStrategy<T>, TimeBasedRoutingSupportCallback<T> {

  private static final int NOT_SET = -1;

  private final StdWorkerState<T> state;
  private final TimeBasedRoutingSupport<T> routingSupport;
  private final TransitCalculator<T> calculator;

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
    this.state = state;
    this.routingSupport =
      new TimeBasedRoutingSupport<>(slackProvider, calculator, roundProvider, lifecycle);
    this.routingSupport.withCallback(this);
    this.calculator = calculator;
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    int departureTime = routingSupport.getTimeDependentDepartureTime(
      accessPath,
      iterationDepartureTime
    );

    // This access is not available after the iteration departure time
    if (departureTime == -1) {
      return;
    }

    state.setAccessToStop(accessPath, departureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    routingSupport.prepareForTransitWith(timeTable);
    this.onTripIndex = UNBOUNDED_TRIP_INDEX;
    this.onTripBoardTime = NOT_SET;
    this.onTripBoardStop = NOT_SET;
    this.onTrip = null;
  }

  @Override
  public void alight(final int stopIndex, final int stopPos, final int alightSlack) {
    if (onTripIndex != UNBOUNDED_TRIP_INDEX) {
      final int stopArrivalTime = calculator.stopArrivalTime(onTrip, stopPos, alightSlack);
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

  /* TimeBasedRoutingSupportCallback */

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

  private int prevArrivalTime(int stopIndex) {
    return state.bestTimePreviousRound(stopIndex);
  }

  private TransitArrival<T> previousTransitArrival(int boardStopIndex) {
    return state.previousTransit(boardStopIndex);
  }
}
