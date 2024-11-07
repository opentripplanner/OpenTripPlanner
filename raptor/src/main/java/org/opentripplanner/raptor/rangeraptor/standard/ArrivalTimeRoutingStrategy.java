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
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(ArrivalTimeRoutingStrategy.class);

  private static final int NOT_SET = -1;

  private final StdWorkerState<T> state;
  private final TimeBasedBoardingSupport<T> boardingSupport;
  private final TransitCalculator<T> calculator;

  private int logCount = 0;
  private int onTripIndex;
  private int onTripBoardTime;
  private int onTripBoardStop;
  private T onTrip;

  public ArrivalTimeRoutingStrategy(
    StdWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    TransitCalculator<T> calculator
  ) {
    this.state = state;
    this.boardingSupport = boardingSupport;
    this.calculator = calculator;
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    state.setAccessToStop(accessPath, departureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorRoute<T> route) {
    boardingSupport.prepareForTransitWith(route.timetable());
    this.onTripIndex = UNBOUNDED_TRIP_INDEX;
    this.onTripBoardTime = NOT_SET;
    this.onTripBoardStop = NOT_SET;
    this.onTrip = null;
  }

  @Override
  public void alightOnlyRegularTransferExist(int stopIndex, int stopPos, int alightSlack) {
    if (onTripIndex != UNBOUNDED_TRIP_INDEX) {
      final int stopArrivalTime = calculator.stopArrivalTime(onTrip, stopPos, alightSlack);

      // TODO: Make sure that the TimeTables can not have negative trip times, then
      //       this check can be removed.
      if (calculator.isBefore(stopArrivalTime, onTripBoardTime)) {
        logInvalidAlightTime(stopPos, stopArrivalTime);
      } else {
        state.transitToStop(stopIndex, stopArrivalTime, onTripBoardStop, onTripBoardTime, onTrip);
      }
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
    int prevArrivalTime = prevArrivalTime(stopIndex);
    var boarding = boardingSupport.searchRegularTransfer(
      prevArrivalTime,
      stopPos,
      boardSlack,
      onTripIndex
    );
    if (!boarding.empty()) {
      board(stopIndex, boarding);
    }
  }

  @Override
  public void boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
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

  private void board(int stopIndex, RaptorBoardOrAlightEvent<T> boarding) {
    onTripIndex = boarding.tripIndex();
    onTrip = boarding.trip();
    onTripBoardTime = boarding.time();
    onTripBoardStop = stopIndex;
  }

  private int prevArrivalTime(int stopIndex) {
    return state.bestTimePreviousRound(stopIndex);
  }

  private TransitArrival<T> previousTransitArrival(int boardStopIndex) {
    return state.previousTransit(boardStopIndex);
  }

  private void logInvalidAlightTime(int stopPos, int stopArrivalTime) {
    if (logCount < 3) {
      ++logCount;
      LOG.error(
        "Traveling back in time is not allowed. Board stop pos: {}, alight stop pos: {}, stop arrival time: {}, trip: {}.",
        onTrip.findDepartureStopPosition(onTripBoardTime, onTripBoardStop),
        stopPos,
        stopArrivalTime,
        onTrip
      );
    }
  }
}
