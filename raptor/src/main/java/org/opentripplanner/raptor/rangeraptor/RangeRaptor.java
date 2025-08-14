package org.opentripplanner.raptor.rangeraptor;

import static java.util.Objects.requireNonNull;

import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouter;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
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
 * This class originated as a rewrite of Conveyals RAPTOR code: https://github.com/conveyal/r5.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@SuppressWarnings("Duplicates")
public final class RangeRaptor<T extends RaptorTripSchedule> implements RaptorRouter<T> {

  private final RangeRaptorWorker<T> worker;

  /**
   * The round tracker keep track for the current Raptor round, and abort the search if the round
   * max limit is reached.
   */
  private final RoundTracker roundTracker;

  private final RaptorTransitDataProvider<T> transitData;

  private final RaptorTransitCalculator<T> calculator;

  private final RaptorTimers timers;

  private final AccessPaths accessPaths;

  private final LifeCycleEventPublisher lifeCycle;

  private final Runnable timeoutHook;

  private final int minNumberOfRounds;

  public RangeRaptor(
    RangeRaptorWorker<T> worker,
    RaptorTransitDataProvider<T> transitData,
    AccessPaths accessPaths,
    RoundTracker roundTracker,
    RaptorTransitCalculator<T> calculator,
    LifeCycleEventPublisher lifeCyclePublisher,
    RaptorTimers timers,
    Runnable timeoutHook
  ) {
    this.worker = requireNonNull(worker);
    this.transitData = requireNonNull(transitData);
    this.calculator = requireNonNull(calculator);
    this.timers = requireNonNull(timers);
    this.accessPaths = requireNonNull(accessPaths);
    this.minNumberOfRounds = accessPaths.calculateMaxNumberOfRides();
    this.roundTracker = requireNonNull(roundTracker);
    this.lifeCycle = requireNonNull(lifeCyclePublisher);
    this.timeoutHook = requireNonNull(timeoutHook);
  }

  public RaptorRouterResult<T> route() {
    timers.route(() -> {
      int iterationDepartureTime = RaptorConstants.TIME_NOT_SET;
      lifeCycle.notifyRouteSearchStart(calculator.searchForward());
      transitData.setup();

      // The main outer loop iterates backward over all minutes in the departure times window.
      // Ergo, we re-use the arrival times found in searches that have already occurred that
      // depart later, because the arrival time given departure at time t is upper-bounded by
      // the arrival time given departure at minute t + 1.
      final IntIterator it = calculator.rangeRaptorMinutes();
      while (it.hasNext()) {
        iterationDepartureTime = it.next();
        runRaptorForMinute(iterationDepartureTime);
      }

      // Iterate over virtual departure times - this is needed to allow access with a time-penalty
      // which falls outside the search-window due to the added time-penalty.
      if (!calculator.oneIterationOnly()) {
        final IntIterator as = accessPaths.iterateOverPathsWithPenalty(iterationDepartureTime);
        while (as.hasNext()) {
          iterationDepartureTime = as.next();
          runRaptorForMinute(iterationDepartureTime);
        }
      }
    });
    return worker.result();
  }

  /**
   * Perform one minute of a RAPTOR search.
   */
  private void runRaptorForMinute(int iterationDepartureTime) {
    setupIteration(iterationDepartureTime);
    worker.findAccessOnStreetForRound();

    while (hasMoreRounds()) {
      lifeCycle.prepareForNextRound(roundTracker.nextRound());

      // NB since we have transfer limiting not bothering to cut off search when there are no
      // more transfers as that will be rare and complicates the code
      worker.findTransitForRound();
      lifeCycle.transitsForRoundComplete();

      worker.findAccessOnBoardForRound();

      worker.findTransfersForRound();
      lifeCycle.transfersForRoundComplete();

      lifeCycle.roundComplete(worker.isDestinationReachedInCurrentRound());

      worker.findAccessOnStreetForRound();
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
    return worker.hasMoreRounds() && roundTracker.hasMoreRounds();
  }

  private int round() {
    return roundTracker.round();
  }

  /**
   * Run the raptor search for this particular iteration departure time
   */
  private void setupIteration(int iterationDepartureTime) {
    timeoutHook.run();
    roundTracker.setupIteration();
    lifeCycle.prepareForNextRound(round());
    lifeCycle.setupIteration(iterationDepartureTime);
  }
}
