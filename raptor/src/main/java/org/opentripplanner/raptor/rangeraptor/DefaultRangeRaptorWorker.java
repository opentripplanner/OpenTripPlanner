package org.opentripplanner.raptor.rangeraptor;

import java.util.Collection;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripPattern;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * The algorithm used herein is described in
 * <p>
 * Conway, Matthew Wigginton, Andrew Byrd, and Marco van der Linden. “Evidence-Based Transit and
 * Land Use Sketch Planning Using Interactive Accessibility Methods on Combined Schedule and
 * Headway-Based Networks.” Transportation Research Record 2653 (2017). doi:10.3141/2653-06.
 * <p>
 * <a href="https://www.microsoft.com/en-us/research/wp-content/uploads/2012/01/raptor_alenex.pdf">
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
 * This class originated as a rewrite of <a href="https://github.com/conveyal/r5">Conveyals RAPTOR
 * code</a>.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@SuppressWarnings("Duplicates")
public final class DefaultRangeRaptorWorker<T extends RaptorTripSchedule>
  implements RangeRaptorWorker<T> {

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

  private final RaptorTransitDataProvider<T> transitData;

  private final SlackProvider slackProvider;

  private final RaptorTransitCalculator<T> calculator;

  private final RaptorTimers timers;

  private final AccessPaths accessPaths;

  private final boolean enableTransferConstraints;

  private int iterationDepartureTime;

  private int round;

  /**
   * @param accessPaths can be null in case the worker is chained - only the first worker has
   *                    access.
   */
  public DefaultRangeRaptorWorker(
    RaptorWorkerState<T> state,
    RoutingStrategy<T> transitWorker,
    RaptorTransitDataProvider<T> transitData,
    SlackProvider slackProvider,
    AccessPaths accessPaths,
    RaptorTransitCalculator<T> calculator,
    WorkerLifeCycle lifeCycle,
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
    this.enableTransferConstraints = enableTransferConstraints;

    lifeCycle.onSetupIteration(time -> this.iterationDepartureTime = time);
    lifeCycle.onPrepareForNextRound(round -> this.round = round);
  }

  /**
   * Check if the RangeRaptor should continue with a new round.
   */
  @Override
  public boolean hasMoreRounds() {
    return state.isNewRoundAvailable();
  }

  @Override
  public void applyAccessArrivedOnStreet() {
    addAccessPaths(accessPaths.arrivedOnStreetByNumOfRides(round));
  }

  @Override
  public void applyAccessArrivedOnBoard() {
    addAccessPaths(accessPaths.arrivedOnBoardByNumOfRides(round));
  }

  @Override
  public void applyAccessStartOnBoard() {
    for (var accessPath : accessPaths.listStartOnBoardAccesses()) {
      var boarding = accessPath.tripBoarding();
      var route = transitData.getRouteForIndex(boarding.routeIndex());
      var trip = route.timetable().getTripSchedule(boarding.tripScheduleIndex());
      var boardTime = trip.departure(boarding.stopPositionInPattern());

      if (calculator.isInIteration(boardTime, iterationDepartureTime)) {
        transitWorker.addStartOnBoardAccessStopArrival(accessPath, boardTime);
      }
    }
  }

  /**
   * Perform a scheduled search
   */
  @Override
  public void routeTransit() {
    timers.routeTransit(() -> {
      IntIterator stops = state.stopsTouchedPreviousRound();
      IntIterator routeIndexIterator = transitData.routeIndexIterator(stops);

      while (routeIndexIterator.hasNext()) {
        var routeIndex = routeIndexIterator.next();
        var route = transitData.getRouteForIndex(routeIndex);
        var pattern = route.pattern();
        var stopPositions = calculator.patternStopIterator(pattern.numberOfStopsInPattern());

        boardAndAlightRoute(routeIndex, route, pattern, stopPositions);
      }
      // Called here (per worker) rather than via the lifecycle event, because a via search has
      // multiple workers sharing the same lifecycle. Via search events MUST be propagated before
      // the next segment do the transit routing.
      state.transitsForRoundComplete();
    });
  }

  @Override
  public void applyTransfers() {
    timers.applyTransfers(() -> {
      IntIterator it = state.stopsTouchedByTransitCurrentRound();

      while (it.hasNext()) {
        final int fromStop = it.next();
        // no need to consider loop transfers, since we don't mark patterns here any more
        // loop transfers are already included by virtue of those stops having been reached
        state.transferToStops(fromStop, calculator.getTransfers(transitData, fromStop));
      }
    });
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    return state.isDestinationReachedInCurrentRound();
  }

  @Override
  public RaptorRouterResult<T> result() {
    return state.results();
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
        transitWorker.addAccessStopArrival(it, departureTime);
      }
    }
  }

  private void boardAndAlightRoute(
    int routeIndex,
    RaptorRoute<T> route,
    RaptorTripPattern pattern,
    IntIterator stopPositions
  ) {
    var txSearch = enableTransferConstraints
      ? calculator.transferConstraintsSearch(transitData, routeIndex)
      : null;

    int alightSlack = slackProvider.alightSlack(pattern.slackIndex());
    int boardSlack = slackProvider.boardSlack(pattern.slackIndex());

    transitWorker.prepareForTransitWith(route);

    var onBoardArrivals = transitWorker.consumeStartOnBoardStopArrivalsForRoute(routeIndex);

    while (stopPositions.hasNext()) {
      int stopPos = stopPositions.next();
      int stopIndex = pattern.stopIndex(stopPos);

      // attempt to alight if we're on board, this is done above the board search
      // so that we don't alight on first stop boarded
      if (calculator.alightingPossibleAt(pattern, stopPos)) {
        if (enableTransferConstraints && txSearch.transferExistSourceStop(stopPos)) {
          transitWorker.alightConstrainedTransferExist(stopIndex, stopPos, alightSlack);
        } else {
          transitWorker.alightOnlyRegularTransferExist(stopIndex, stopPos, alightSlack);
        }
      }

      // attempt to board using on-board trip access/pass-though. This happens after
      // the alight at the given stop position.
      if (onBoardArrivals != null && onBoardArrivals.arrivalExistForStopPosition(stopPos)) {
        for (var arrival : onBoardArrivals.listArrivals(stopPos)) {
          var boarding = arrival.boardingConstraint();
          var trip = route.timetable().getTripSchedule(boarding.tripScheduleIndex());
          transitWorker.boardWithStartOnBoardAccess(
            arrival.accessStopArrival(),
            trip,
            boarding.stopPositionInPattern()
          );
        }
      }

      if (calculator.boardingPossibleAt(pattern, stopPos)) {
        // Don't attempt to board if this stop was not reached in the last round.
        // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
        if (state.isStopReachedInPreviousRound(stopIndex)) {
          // has constrained transfers
          if (enableTransferConstraints && txSearch.transferExistTargetStop(stopPos)) {
            transitWorker.boardWithConstrainedTransfer(stopIndex, stopPos, boardSlack, txSearch);
          } else {
            transitWorker.boardWithRegularTransfer(stopIndex, stopPos, boardSlack);
          }
        }
      }
    }
  }
}
