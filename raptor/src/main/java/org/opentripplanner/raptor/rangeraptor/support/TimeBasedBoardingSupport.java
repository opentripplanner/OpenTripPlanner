package org.opentripplanner.raptor.rangeraptor.support;

import static org.opentripplanner.raptor.rangeraptor.transit.RoundTracker.isFirstRound;
import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.raptor.api.view.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;

/**
 * Shared boarding logic for all time-dependent {@link RoutingStrategy}s.
 * <p>
 * Handles trip search creation and earliest-board-time calculation for both regular and
 * constrained (guaranteed/stay-seated) boardings. The choice between an exact trip search
 * and a standard trip search depends on whether this is the first iteration and the first
 * round, and whether time-dependent access is in use.
 */
public final class TimeBasedBoardingSupport<T extends RaptorTripSchedule> {

  private final SlackProvider slackProvider;
  private final RaptorTransitCalculator<T> calculator;
  private final boolean hasTimeDependentAccess;

  private boolean inFirstIteration = true;
  private RaptorTimeTable<T> timeTable;
  private RaptorTripScheduleSearch<T> tripSearch;
  private int round;

  /**
   * @param hasTimeDependentAccess {@code true} if any access path is time-dependent, which
   *                               disables the exact-trip-search optimisation in round 1
   * @param calculator             direction-aware arithmetic for forward/reverse searches
   * @param subscriptions          worker lifecycle hooks used to track iteration and round state
   */
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

  /**
   * Set the timetable for the pattern about to be boarded and create a suitable trip search.
   * Must be called once per pattern before any {@code searchForBoarding} calls.
   */
  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    this.timeTable = timeTable;
    this.tripSearch = createTripSearch(timeTable);
  }

  /**
   * Same as {@link #searchForRegularBoarding(int, int, int, int)}, but with
   * {@code onTripIndex} unbounded.
   */
  public RaptorBoardOrAlightEvent<T> searchForRegularBoarding(
    int prevArrivalTime,
    int stopPos,
    int boardSlack
  ) {
    return searchForRegularBoarding(prevArrivalTime, stopPos, boardSlack, UNBOUNDED_TRIP_INDEX);
  }

  /**
   * Search for the earliest trip that can be boarded at {@code stopPos}, applying
   * {@code boardSlack} to the previous arrival time to derive the earliest board time.
   *
   * @param prevArrivalTime the time at which the traveller arrives at the board stop
   * @param stopPos         the stop position within the pattern's stop sequence
   * @param boardSlack      minimum seconds between arrival and boarding
   * @param onTripIndex     upper bound on the trip index (used when already on a trip);
   *                        pass {@link RaptorTripScheduleSearch#UNBOUNDED_TRIP_INDEX} for no bound
   * @return the board event, or an empty event if no trip is found
   */
  public RaptorBoardOrAlightEvent<T> searchForRegularBoarding(
    int prevArrivalTime,
    int stopPos,
    int boardSlack,
    int onTripIndex
  ) {
    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);
    return tripSearch.search(earliestBoardTime, stopPos, onTripIndex);
  }

  /**
   * Search for a constrained boarding (stay-seated or guaranteed transfer) at the current stop.
   * <p>
   * If {@code prevTransitStopArrival} is {@code null} — meaning the traveller did not arrive by
   * transit — no constrained transfer can apply, and an empty event is returned.
   * <p>
   * When a previous transit arrival exists, the alight slack of that leg is subtracted to derive
   * the actual transit arrival time, which is passed to the constrained transfer search together
   * with the earliest board time computed from {@code prevArrivalTime} and {@code boardSlack}.
   *
   * @param prevTransitStopArrival the transit arrival that is the source of a potential constrained
   *                               transfer; {@code null} if the traveller did not arrive by transit
   * @param prevArrivalTime        the wall-clock arrival time at the board stop; may differ from
   *                               {@code prevTransitStopArrival} when a walking transfer bridges
   *                               the two stops
   * @param boardSlack             minimum seconds between {@code prevArrivalTime} and boarding;
   *                               stay-seated and guaranteed transfers may override this
   * @param txSearch               the constrained transfer search for the target pattern
   * @return the constrained board event, or an empty event if no constrained transfer is found
   */
  public RaptorBoardOrAlightEvent<T> searchForConstrainedBoarding(
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
   * Compute the earliest time at which a trip may be boarded.
   * <p>
   * In a forward search this adds board slack; in a reverse search it subtracts alight slack.
   * The direction-specific arithmetic is delegated to the {@link RaptorTransitCalculator}.
   */
  private int earliestBoardTime(int prevArrivalTime, int boardSlack) {
    return calculator.plusDuration(prevArrivalTime, boardSlack);
  }

  /**
   * Create a trip search appropriate for the current iteration and round.
   * <p>
   * For every iteration after the first, and only in round 1, an <em>exact</em> trip search is
   * used when there is no time-dependent access. This restricts the first departure to fall within
   * the iteration's time-window — preventing the access path from being time-shifted past the
   * previous iteration's departure time. The restriction also enables departure-time to be used
   * as a pareto criterion in timetable view.
   * <p>
   * The optimisation is skipped for the first iteration because a traveller could board a bus,
   * ride one stop, walk back to the origin, and then wait to board a later trip — results that
   * would correctly be dominated by later iterations, but only after those iterations run.
   */
  private RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
    if (!inFirstIteration && isFirstRound(round) && !hasTimeDependentAccess) {
      return calculator.createExactTripSearch(timeTable);
    }
    return calculator.createTripSearch(timeTable);
  }
}
