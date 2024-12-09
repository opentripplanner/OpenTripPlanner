package org.opentripplanner.raptor.rangeraptor.support;

import static org.opentripplanner.raptor.rangeraptor.transit.RoundTracker.isFirstRound;
import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;

/**
 * This class contains code which is shared by all time-dependent {@link RoutingStrategy}s.
 *
 */
public final class TimeBasedBoardingSupport<T extends RaptorTripSchedule> {

  private final SlackProvider slackProvider;
  private final RaptorTransitCalculator<T> calculator;
  private final boolean hasTimeDependentAccess;
  private boolean inFirstIteration = true;
  private RaptorTimeTable<T> timeTable;
  private RaptorTripScheduleSearch<T> tripSearch;
  private int round;

  public TimeBasedBoardingSupport(
    boolean hasTimeDependentAccess,
    SlackProvider slackProvider,
    RaptorTransitCalculator<T> calculator,
    WorkerLifeCycle subscriptions
  ) {
    this.hasTimeDependentAccess = hasTimeDependentAccess;
    this.slackProvider = slackProvider;
    this.calculator = calculator;

    subscriptions.onIterationComplete(() -> inFirstIteration = false);
    subscriptions.onPrepareForNextRound(r -> this.round = r);
  }

  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    this.timeTable = timeTable;
    this.tripSearch = createTripSearch(timeTable);
  }

  /**
   * Same as {@link #searchRegularTransfer(int, int, int, int)}, but with
   * {@code onTripIndex} unbounded.
   */
  public RaptorBoardOrAlightEvent<T> searchRegularTransfer(
    int prevArrivalTime,
    int stopPos,
    int boardSlack
  ) {
    return searchRegularTransfer(prevArrivalTime, stopPos, boardSlack, UNBOUNDED_TRIP_INDEX);
  }

  public RaptorBoardOrAlightEvent<T> searchRegularTransfer(
    int prevArrivalTime,
    int stopPos,
    int boardSlack,
    int onTripIndex
  ) {
    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);
    return tripSearch.search(earliestBoardTime, stopPos, onTripIndex);
  }

  /**
   *
   * @param prevTransitStopArrival the current boarding previous transit arrival. This is used to
   *                               look up any guaranteed transfers.
   * @param prevArrivalTime        the arrival time for the board stop ({@code stopIndex}), this
   *                               may not be same as the {@code prevTransitStopArrival}, since
   *                               there might be a "walking" transfer to reach stop.
   * @param boardSlack             The minimum number of seconds to apply to the arrival time
   *                               before boarding a trip. Stay-seated and guaranteed transfers
   *                               may override this.
   * @param txSearch               The constrained transfer search to use.
   */
  public RaptorBoardOrAlightEvent<T> searchConstrainedTransfer(
    TransitArrival<T> prevTransitStopArrival,
    int prevArrivalTime,
    int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  ) {
    // Get the previous transit stop arrival (transfer source)
    if (prevTransitStopArrival == null) {
      return RaptorBoardOrAlightEvent.empty(earliestBoardTime(prevArrivalTime, boardSlack));
    }

    int prevTransitStopArrivalTime = prevTransitStopArrival.arrivalTime();

    int prevTransitArrivalTime = calculator.minusDuration(
      prevTransitStopArrivalTime,
      slackProvider.alightSlack(prevTransitStopArrival.trip().pattern().slackIndex())
    );

    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);

    return txSearch.find(
      timeTable,
      slackProvider.transferSlack(),
      prevTransitStopArrival.trip(),
      prevTransitStopArrival.stop(),
      prevTransitArrivalTime,
      earliestBoardTime
    );
  }

  /**
   * Add board-slack(forward-search) or alight-slack(reverse-search)
   */
  private int earliestBoardTime(int prevArrivalTime, int boardSlack) {
    return calculator.plusDuration(prevArrivalTime, boardSlack);
  }

  private RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
    if (!inFirstIteration && isFirstRound(round) && !hasTimeDependentAccess) {
      // For the first round of every iteration(except the first) we restrict the first
      // departure to happen within the time-window of the iteration. Another way to put this,
      // is to say that we allow for the access path to be time-shifted to a later departure,
      // but not past the previous iteration departure time. This save a bit of processing,
      // but most importantly allow us to use the departure-time as a pareto criteria in
      // time-table view. This is not valid for the first iteration, because we could jump on
      // a bus, take it one stop and walk back and then wait to board a later trip - this kind
      // of results would be rejected by earlier iterations, for all iterations except the
      // first.
      return calculator.createExactTripSearch(timeTable);
    }

    // Default: create a standard trip search
    return calculator.createTripSearch(timeTable);
  }
}
