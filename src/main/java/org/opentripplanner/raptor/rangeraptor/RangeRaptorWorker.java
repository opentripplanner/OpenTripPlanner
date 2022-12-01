package org.opentripplanner.raptor.rangeraptor;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.Worker;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerState;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.RoundTracker;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.spi.TransitArrival;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleBoardSearch;

/**
 * The algorithm used herein is described in
 * <p>
 * Conway, Matthew Wigginton, Andrew Byrd, and Marco van der Linden. “Evidence-Based Transit and
 * Land Use Sketch Planning Using Interactive Accessibility Methods on Combined Schedule and
 * Headway-Based Networks.” Transportation Research Record 2653 (2017). doi:10.3141/2653-06.
 * <p>
 * Delling, Daniel, Thomas Pajor, and Renato Werneck. “Round-Based Public Transit Routing,”
 * January 1, 2012. http://research.microsoft.com/pubs/156567/raptor_alenex.pdf.
 * <p>
 * This version do support the following features:
 * <ul>
 *     <li>Raptor (R)
 *     <li>Range Raptor (RR)
 *     <li>Multi-criteria pareto optimal Range Raptor (McRR)
 *     <li>Reverse search in combination with R and RR
 * </ul>
 * This version does NOT support the following features:
 * <ul>
 *     <li>Frequency routes, supported by the original code using Monte Carlo methods
 *     (generating randomized schedules)
 * </ul>
 * <p>
 * This class originated as a rewrite of Conveyals RAPTOR code: https://github.com/conveyal/r5.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@SuppressWarnings("Duplicates")
public final class RangeRaptorWorker<T extends RaptorTripSchedule> implements Worker<T> {

  private final RoutingStrategy<T> transitWorker;

  /**
   * The RangeRaptor state - we delegate keeping track of state to the state object, this allows the
   * worker implementation to focus on the algorithm, while the state keep track of the result.
   * <p/>
   * This also allow us to try out different strategies for storing the result in memory. For a long
   * time we had a state which stored all data as int arrays in addition to the current
   * object-oriented approach. There were no performance differences(=> GC is not the bottle neck),
   * so we dropped the integer array implementation.
   */
  private final WorkerState<T> state;

  /**
   * The round tracker keep track for the current Raptor round, and abort the search if the round
   * max limit is reached.
   */
  private final RoundTracker roundTracker;

  private final RaptorTransitDataProvider<T> transitData;

  private final SlackProvider slackProvider;

  private final TransitCalculator<T> calculator;

  private final RaptorTimers timers;

  private final AccessPaths accessPaths;

  private final LifeCycleEventPublisher lifeCycle;

  private final int minNumberOfRounds;

  private final boolean enableTransferConstraints;

  private boolean inFirstIteration = true;

  private boolean hasTimeDependentAccess = false;

  private int iterationDepartureTime;

  public RangeRaptorWorker(
    WorkerState<T> state,
    RoutingStrategy<T> transitWorker,
    RaptorTransitDataProvider<T> transitData,
    SlackProvider slackProvider,
    AccessPaths accessPaths,
    RoundProvider roundProvider,
    TransitCalculator<T> calculator,
    LifeCycleEventPublisher lifeCyclePublisher,
    RaptorTimers timers,
    boolean enableTransferConstraints
  ) {
    this.transitWorker = transitWorker;
    this.state = state;
    this.transitData = transitData;
    this.slackProvider = slackProvider;
    this.calculator = calculator;
    this.timers = timers;
    this.accessPaths = accessPaths;
    this.minNumberOfRounds = accessPaths.calculateMaxNumberOfRides();
    this.enableTransferConstraints = enableTransferConstraints;

    // We do a cast here to avoid exposing the round tracker  and the life cycle publisher to
    // "everyone" by providing access to it in the context.
    this.roundTracker = (RoundTracker) roundProvider;
    this.lifeCycle = lifeCyclePublisher;
  }

  /**
   * For each iteration (minute), calculate the minimum travel time to each transit stop in
   * seconds.
   * <p/>
   * Run the scheduled search, round 0 is the street search
   * <p/>
   * We are using the Range-RAPTOR extension described in Delling, Daniel, Thomas Pajor, and Renato
   * Werneck. “Round-Based Public Transit Routing,” January 1, 2012.
   * http://research.microsoft.com/pubs/156567/raptor_alenex.pdf.
   *
   */
  @Override
  public void route() {
    timers.route(() -> {
      lifeCycle.notifyRouteSearchStart(calculator.searchForward());
      transitData.setup();

      // The main outer loop iterates backward over all minutes in the departure times window.
      // Ergo, we re-use the arrival times found in searches that have already occurred that
      // depart later, because the arrival time given departure at time t is upper-bounded by
      // the arrival time given departure at minute t + 1.
      final IntIterator it = calculator.rangeRaptorMinutes();
      while (it.hasNext()) {
        // Run the raptor search for this particular iteration departure time
        iterationDepartureTime = it.next();
        lifeCycle.setupIteration(iterationDepartureTime);
        runRaptorForMinute();
        inFirstIteration = false;
      }
    });
  }

  @Override
  public Collection<Path<T>> paths() {
    return state.extractPaths();
  }

  @Override
  public StopArrivals stopArrivals() {
    return state.extractStopArrivals();
  }

  /**
   * Perform one minute of a RAPTOR search.
   */
  private void runRaptorForMinute() {
    findAccessOnStreetForRound();

    while (hasMoreRounds()) {
      lifeCycle.prepareForNextRound(roundTracker.nextRound());

      // NB since we have transfer limiting not bothering to cut off search when there are no
      // more transfers as that will be rare and complicates the code
      findTransitForRound();

      findAccessOnBoardForRound();

      findTransfersForRound();

      lifeCycle.roundComplete(state.isDestinationReachedInCurrentRound());

      findAccessOnStreetForRound();
    }

    // This state is repeatedly modified as the outer loop progresses over departure minutes.
    // We have to be careful here, the next iteration will modify the state, so we need to make
    // protective copies of any information we want to retain.
    lifeCycle.iterationComplete();
  }

  /**
   * Check if the RangeRaptor should continue with a new round.
   */
  private boolean hasMoreRounds() {
    if (round() < minNumberOfRounds) {
      return true;
    }
    return state.isNewRoundAvailable() && roundTracker.hasMoreRounds();
  }

  /**
   * Perform a scheduled search
   */
  private void findTransitForRound() {
    timers.findTransitForRound(() -> {
      IntIterator stops = state.stopsTouchedPreviousRound();
      IntIterator routeIndexIterator = transitData.routeIndexIterator(stops);

      while (routeIndexIterator.hasNext()) {
        var routeIndex = routeIndexIterator.next();
        var route = transitData.getRouteForIndex(routeIndex);
        var pattern = route.pattern();
        var tripSearch = createTripSearch(route.timetable());
        var txSearch = enableTransferConstraints
          ? calculator.transferConstraintsSearch(transitData, routeIndex)
          : null;

        int alightSlack = slackProvider.alightSlack(pattern.slackIndex());
        int boardSlack = slackProvider.boardSlack(pattern.slackIndex());

        transitWorker.prepareForTransitWith();

        IntIterator stop = calculator.patternStopIterator(pattern.numberOfStopsInPattern());

        while (stop.hasNext()) {
          int stopPos = stop.next();
          int stopIndex = pattern.stopIndex(stopPos);

          // attempt to alight if we're on board, this is done above the board search
          // so that we don't alight on first stop boarded
          if (calculator.alightingPossibleAt(pattern, stopPos)) {
            transitWorker.alight(stopIndex, stopPos, alightSlack);
          }

          if (calculator.boardingPossibleAt(pattern, stopPos)) {
            // MC Raptor have many, while RR have one boarding
            transitWorker.forEachBoarding(
              stopIndex,
              (int prevArrivalTime) -> {
                boolean boardedUsingConstrainedTransfer =
                  enableTransferConstraints &&
                  boardWithConstrainedTransfer(
                    txSearch,
                    route.timetable(),
                    stopIndex,
                    stopPos,
                    prevArrivalTime,
                    boardSlack
                  );

                // Find the best trip and board [no guaranteed transfer exist]
                if (!boardedUsingConstrainedTransfer) {
                  boardWithRegularTransfer(
                    tripSearch,
                    stopIndex,
                    stopPos,
                    prevArrivalTime,
                    boardSlack
                  );
                }
              }
            );
          }
        }
      }
      lifeCycle.transitsForRoundComplete();
    });
  }

  private void boardWithRegularTransfer(
    RaptorTripScheduleSearch<T> tripSearch,
    int stopIndex,
    int stopPos,
    int prevArrivalTime,
    int boardSlack
  ) {
    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);
    // check if we can back up to an earlier trip due to this stop
    // being reached earlier
    var result = tripSearch.search(earliestBoardTime, stopPos, transitWorker.onTripIndex());
    if (result != null) {
      transitWorker.board(stopIndex, earliestBoardTime, result);
    } else {
      transitWorker.boardSameTrip(earliestBoardTime, stopPos, stopIndex);
    }
  }

  /**
   * @return {@code true} if a constrained transfer exist to prevent the normal trip search from
   * execution.
   */
  private boolean boardWithConstrainedTransfer(
    @Nonnull RaptorConstrainedTripScheduleBoardingSearch<T> txSearch,
    RaptorTimeTable<T> targetTimetable,
    int targetStopIndex,
    int targetStopPos,
    int prevArrivalTime,
    int boardSlack
  ) {
    if (!txSearch.transferExist(targetStopPos)) {
      return false;
    }

    // Get the previous transit stop arrival (transfer source)
    TransitArrival<T> sourceStopArrival = transitWorker.previousTransit(targetStopIndex);
    if (sourceStopArrival == null) {
      return false;
    }

    int prevTransitStopArrivalTime = sourceStopArrival.arrivalTime();

    int prevTransitArrivalTime = calculator.minusDuration(
      prevTransitStopArrivalTime,
      slackProvider.alightSlack(sourceStopArrival.trip().pattern().slackIndex())
    );

    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);

    var result = txSearch.find(
      targetTimetable,
      slackProvider.transferSlack(),
      sourceStopArrival.trip(),
      sourceStopArrival.stop(),
      prevTransitArrivalTime,
      earliestBoardTime
    );

    if (result == null) {
      return false;
    }

    var constraint = result.getTransferConstraint();

    if (constraint.isNotAllowed()) {
      // We are blocking a normal trip search here by returning
      // true without boarding the trip
      return true;
    }

    transitWorker.board(
      targetStopIndex,
      result.getEarliestBoardTimeForConstrainedTransfer(),
      result
    );

    return true;
  }

  private void findTransfersForRound() {
    timers.findTransfersForRound(() -> {
      IntIterator it = state.stopsTouchedByTransitCurrentRound();

      while (it.hasNext()) {
        final int fromStop = it.next();
        // no need to consider loop transfers, since we don't mark patterns here any more
        // loop transfers are already included by virtue of those stops having been reached
        state.transferToStops(fromStop, calculator.getTransfers(transitData, fromStop));
      }

      lifeCycle.transfersForRoundComplete();
    });
  }

  /**
   * Create a trip search using {@link TripScheduleBoardSearch}.
   * <p/>
   * This is protected to allow reverse search to override and create a alight search instead.
   */
  private RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
    if (!inFirstIteration && roundTracker.isFirstRound() && !hasTimeDependentAccess) {
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

  private void findAccessOnStreetForRound() {
    addAccessPaths(accessPaths.arrivedOnStreetByNumOfRides().get(round()));
  }

  private void findAccessOnBoardForRound() {
    addAccessPaths(accessPaths.arrivedOnBoardByNumOfRides().get(round()));
  }

  /**
   * Set the departure time in the scheduled search to the given departure time, and prepare for the
   * scheduled search at the next-earlier minute.
   */
  private void addAccessPaths(Collection<RaptorAccessEgress> accessPaths) {
    if (accessPaths == null) {
      return;
    }

    for (RaptorAccessEgress it : accessPaths) {
      // Earliest possible departure time from the origin, or latest possible arrival
      // time at the destination if searching backwards.
      int timeDependentDepartureTime = calculator.departureTime(it, iterationDepartureTime);

      // This access is not available after the iteration departure time
      if (timeDependentDepartureTime == -1) {
        continue;
      }

      // If the time differs from the iterationDepartureTime, than the access has time
      // restrictions. If the difference between _any_ access between iterations is not a
      // uniform iterationStep, than the exactTripSearch optimisation may not be used.
      if (timeDependentDepartureTime != iterationDepartureTime) {
        hasTimeDependentAccess = true;
      }

      transitWorker.setAccessToStop(it, iterationDepartureTime, timeDependentDepartureTime);
    }
  }

  private int round() {
    return roundTracker.round();
  }

  /**
   * Add board-slack(forward-search) or alight-slack(reverse-search)
   */
  private int earliestBoardTime(int prevArrivalTime, int boardSlack) {
    return calculator.plusDuration(prevArrivalTime, boardSlack);
  }
}
