package org.opentripplanner.raptor.rangeraptor;

import java.util.Collection;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.RoundTracker;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

/**
 * The algorithm used herein is described in
 * <p>
 * Conway, Matthew Wigginton, Andrew Byrd, and Marco van der Linden. “Evidence-Based Transit and
 * Land Use Sketch Planning Using Interactive Accessibility Methods on Combined Schedule and
 * Headway-Based Networks.” Transportation Research Record 2653 (2017). doi:10.3141/2653-06.
 * <p>
 * <a href="http://research.microsoft.com/pubs/156567/raptor_alenex.pdf">
 *   Delling, Daniel, Thomas Pajor, and Renato Werneck. “Round-Based Public Transit Routing”,
 *   January 1, 2012.
 * </a>.
 * <p>
 * This version supports the following features:
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
public final class DefaultRangeRaptorWorker<T extends RaptorTripSchedule>
  implements RaptorWorker<T> {

  private final RoutingStrategy<T> transitWorker;

  /**
   * The RangeRaptor state - we delegate keeping track of state to the state object, this allows
   * the worker implementation to focus on the algorithm, while the state keep track of the result.
   * <p/>
   * This also allows us to try out different strategies for storing the result in memory. For a
   * long time, we had a state which stored all data as int arrays in addition to the current
   * object-oriented approach. There were no performance differences(=> GC is not the bottleneck),
   * so we dropped the integer array implementation.
   */
  private final RaptorWorkerState<T> state;

  /**
   * The round tracker keep track for the current Raptor round, and abort the search if the round
   * max limit is reached.
   */
  private final RoundTracker roundTracker;

  private final RaptorTransitDataProvider<T> transitData;

  private final SlackProvider slackProvider;

  private final RaptorTransitCalculator<T> calculator;

  private final RaptorTimers timers;

  private final AccessPaths accessPaths;

  private final LifeCycleEventPublisher lifeCycle;

  private final int minNumberOfRounds;

  private final boolean enableTransferConstraints;

  private int iterationDepartureTime;

  public DefaultRangeRaptorWorker(
    RaptorWorkerState<T> state,
    RoutingStrategy<T> transitWorker,
    RaptorTransitDataProvider<T> transitData,
    SlackProvider slackProvider,
    AccessPaths accessPaths,
    RoundProvider roundProvider,
    RaptorTransitCalculator<T> calculator,
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
   * Run the scheduled search, round 0 is the street search.
   */
  @Override
  public RaptorWorkerResult<T> route() {
    timers.route(() -> {
      lifeCycle.notifyRouteSearchStart(calculator.searchForward());
      transitData.setup();

      // The main outer loop iterates backward over all minutes in the departure times window.
      // Ergo, we re-use the arrival times found in searches that have already occurred that
      // depart later, because the arrival time given departure at time t is upper-bounded by
      // the arrival time given departure at minute t + 1.
      final IntIterator it = calculator.rangeRaptorMinutes();
      while (it.hasNext()) {
        setupIteration(it.next());
        runRaptorForMinute();
      }

      // Iterate over virtual departure times - this is needed to allow access with a time-penalty
      // which falls outside the search-window due to the added time-penalty.
      if (!calculator.oneIterationOnly()) {
        final IntIterator as = accessPaths.iterateOverPathsWithPenalty(iterationDepartureTime);
        while (as.hasNext()) {
          setupIteration(as.next());
          runRaptorForMinute();
        }
      }
    });
    return state.results();
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
        var txSearch = enableTransferConstraints
          ? calculator.transferConstraintsSearch(transitData, routeIndex)
          : null;

        int alightSlack = slackProvider.alightSlack(pattern.slackIndex());
        int boardSlack = slackProvider.boardSlack(pattern.slackIndex());

        transitWorker.prepareForTransitWith(route);

        IntIterator stop = calculator.patternStopIterator(pattern.numberOfStopsInPattern());

        while (stop.hasNext()) {
          int stopPos = stop.next();
          int stopIndex = pattern.stopIndex(stopPos);

          transitWorker.prepareForNextStop(stopIndex, stopPos);

          // attempt to alight if we're on board, this is done above the board search
          // so that we don't alight on first stop boarded
          if (calculator.alightingPossibleAt(pattern, stopPos)) {
            if (enableTransferConstraints && txSearch.transferExistSourceStop(stopPos)) {
              transitWorker.alightConstrainedTransferExist(stopIndex, stopPos, alightSlack);
            } else {
              transitWorker.alightOnlyRegularTransferExist(stopIndex, stopPos, alightSlack);
            }
          }

          if (calculator.boardingPossibleAt(pattern, stopPos)) {
            // Don't attempt to board if this stop was not reached in the last round.
            // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
            if (state.isStopReachedInPreviousRound(stopIndex)) {
              // has constrained transfers
              if (enableTransferConstraints && txSearch.transferExistTargetStop(stopPos)) {
                transitWorker.boardWithConstrainedTransfer(
                  stopIndex,
                  stopPos,
                  boardSlack,
                  txSearch
                );
              } else {
                transitWorker.boardWithRegularTransfer(stopIndex, stopPos, boardSlack);
              }
            }
          }
        }
      }
      lifeCycle.transitsForRoundComplete();
    });
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

  private int round() {
    return roundTracker.round();
  }

  private void findAccessOnStreetForRound() {
    addAccessPaths(accessPaths.arrivedOnStreetByNumOfRides(round()));
  }

  private void findAccessOnBoardForRound() {
    addAccessPaths(accessPaths.arrivedOnBoardByNumOfRides(round()));
  }

  /**
   * Run the raptor search for this particular iteration departure time
   */
  private void setupIteration(int iterationDepartureTime) {
    OTPRequestTimeoutException.checkForTimeout();
    this.iterationDepartureTime = iterationDepartureTime;
    lifeCycle.setupIteration(this.iterationDepartureTime);
  }

  /**
   * Set the departure time in the scheduled search to the given departure time, and prepare for the
   * scheduled search at the next-earlier minute.
   */
  private void addAccessPaths(Collection<RaptorAccessEgress> accessPaths) {
    for (RaptorAccessEgress it : accessPaths) {
      int departureTime = calculator.departureTime(it, iterationDepartureTime);

      // Access must be available after the iteration departure time
      if (departureTime != RaptorConstants.TIME_NOT_SET) {
        transitWorker.setAccessToStop(it, departureTime);
      }
    }
  }
}
