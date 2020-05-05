package org.opentripplanner.transit.raptor.rangeraptor;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.debug.WorkerPerformanceTimers;
import org.opentripplanner.transit.raptor.rangeraptor.transit.RoundTracker;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleBoardSearch;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.util.AvgTimer;

import java.util.Collection;
import java.util.Iterator;


/**
 * The algorithm used herein is described in
 * <p>
 * Conway, Matthew Wigginton, Andrew Byrd, and Marco van der Linden. “Evidence-Based Transit and Land Use Sketch Planning
 * Using Interactive Accessibility Methods on Combined Schedule and Headway-Based Networks.” Transportation Research
 * Record 2653 (2017). doi:10.3141/2653-06.
 * <p>
 * Delling, Daniel, Thomas Pajor, and Renato Werneck. “Round-Based Public Transit Routing,” January 1, 2012.
 * http://research.microsoft.com/pubs/156567/raptor_alenex.pdf.
 * <p>
 * This version do support the following features:
 * <ul>
 *     <li>Raptor (R)
 *     <li>Range Raptor (RR)
 *     <li>Multi-criteria pareto optimal Range Raptor (McRR)
 *     <li>Reverse search in combination with R and RR
 * </ul>
 * This version do NOT support the following features:
 * <ul>
 *     <li>Frequency routes, supported by the original code using Monte Carlo methods (generating randomized schedules)
 * </ul>
 * <p>
 * This class originated as a rewrite of Conveyals RAPTOR code: https://github.com/conveyal/r5.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@SuppressWarnings("Duplicates")
public final class RangeRaptorWorker<T extends RaptorTripSchedule, S extends WorkerState<T>> implements Worker<T> {


    private final RoutingStrategy<T> transitWorker;

    /**
     * The RangeRaptor state - we delegate keeping track of state to the state object,
     * this allow the worker implementation to focus on the algorithm, while
     * the state keep track of the result.
     * <p/>
     * This also allow us to try out different strategies for storing the result in memory.
     * For a long time we had a state witch stored all data as int arrays in addition to the
     * current object-oriented approach. There were no performance differences(=> GC is not
     * the bottle neck), so we dropped the integer array implementation.
     */
    private final S state;

    /**
     * The round tracker keep track for the current Raptor round, and abort the search if the
     * round max limit is reached.
     */
    private final RoundTracker roundTracker;

    private final RaptorTransitDataProvider<T> transitData;

    private final TransitCalculator calculator;

    private final WorkerPerformanceTimers timers;

    private final Collection<RaptorTransfer> accessLegs;

    /**
     * The life cycle is used to publish life cycle events to everyone who
     * listen.
     */
    private final LifeCycleEventPublisher lifeCycle;

    private boolean inFirstIteration = true;


    public RangeRaptorWorker(
            S state,
            RoutingStrategy<T> transitWorker,
            RaptorTransitDataProvider<T> transitData,
            Collection<RaptorTransfer> accessLegs,
            RoundProvider roundProvider,
            TransitCalculator calculator,
            LifeCycleEventPublisher lifeCyclePublisher,
            WorkerPerformanceTimers timers
    ) {
        this.transitWorker = transitWorker;
        this.state = state;
        this.transitData = transitData;
        this.calculator = calculator;
        this.timers = timers;
        this.accessLegs = accessLegs;
        // We do a cast here to avoid exposing the round tracker  and the life cycle publisher to
        // "everyone" by providing access to it in the context.
        this.roundTracker = (RoundTracker) roundProvider;
        this.lifeCycle = lifeCyclePublisher;
    }

    /**
     * For each iteration (minute), calculate the minimum travel time to each transit stop in seconds.
     * <p/>
     * Run the scheduled search, round 0 is the street search
     * <p/>
     * We are using the Range-RAPTOR extension described in Delling, Daniel, Thomas Pajor, and Renato Werneck.
     * “Round-Based Public Transit Routing,” January 1, 2012. http://research.microsoft.com/pubs/156567/raptor_alenex.pdf.
     *
     * @return a unique set of paths
     */
    @Override
    final public Collection<Path<T>> route() {
        timerRoute().time(() -> {
            transitData.setup();

            // The main outer loop iterates backward over all minutes in the departure times window.
            // Ergo, we re-use the arrival times found in searches that have already occurred that
            // depart later, because the arrival time given departure at time t is upper-bounded by
            // the arrival time given departure at minute t + 1.
            final IntIterator it = calculator.rangeRaptorMinutes();
            while (it.hasNext()) {
                // Run the raptor search for this particular iteration departure time
                runRaptorForMinute(it.next());
                inFirstIteration = false;
            }
        });
        return state.extractPaths();
    }

    /**
     * Perform one minute of a RAPTOR search.
     *
     * @param iterationDepartureTime When this search departs.
     */
    private void runRaptorForMinute(int iterationDepartureTime) {
        lifeCycle.setupIteration(iterationDepartureTime);

        doTransfersForAccessLegs(iterationDepartureTime);

        while (hasMoreRounds()) {
            lifeCycle.prepareForNextRound(roundTracker.round());

            // NB since we have transfer limiting not bothering to cut off search when there are no more transfers
            // as that will be rare and complicates the code
            timerByMinuteScheduleSearch().time(this::findAllTransitForRound);

            timerByMinuteTransfers().time(this::transfersForRound);

            lifeCycle.roundComplete(state.isDestinationReachedInCurrentRound());
        }

        // This state is repeatedly modified as the outer loop progresses over departure minutes.
        // We have to be careful here, the next iteration will modify the state, so we need to make
        // protective copies of any information we want to retain.
        lifeCycle.iterationComplete();
    }


    /**
     * Set the departure time in the scheduled search to the given departure time,
     * and prepare for the scheduled search at the next-earlier minute.
     * <p/>
     * This method is protected to allow reverce search to override it.
     */
    private void doTransfersForAccessLegs(int iterationDepartureTime) {
        for (RaptorTransfer it : accessLegs) {
            transitWorker.setInitialTimeForIteration(it, iterationDepartureTime);
        }
    }

    /**
     * Check if the RangeRaptor should continue with a new round.
     */
    private boolean hasMoreRounds() {
        return roundTracker.hasMoreRounds() && state.isNewRoundAvailable();
    }

    /**
     * Perform a scheduled search
     */
    private void findAllTransitForRound() {
        IntIterator stops = state.stopsTouchedPreviousRound();
        Iterator<? extends RaptorRoute<T>> routeIterator = transitData.routeIterator(stops);

        while (routeIterator.hasNext()) {
            RaptorRoute<T> next = routeIterator.next();
            RaptorTripPattern pattern = next.pattern();
            TripScheduleSearch<T> tripSearch = createTripSearch(next.timetable());

            // Prepare for transit
            transitWorker.prepareForTransitWith(pattern, tripSearch);

            // perform transit
            performTransitForRoundAndEachStopInPattern(pattern);
        }
        lifeCycle.transitsForRoundComplete();
    }

    /**
     * Iterate over given pattern and calculate transit for each stop.
     * <p/>
     * This is protected to allow reverse search to override and step backwards.
     */
    private void performTransitForRoundAndEachStopInPattern(final RaptorTripPattern pattern) {
        IntIterator it = calculator.patternStopIterator(pattern.numberOfStopsInPattern());
        while (it.hasNext()) {
            transitWorker.routeTransitAtStop(it.next());
        }
    }

    private void transfersForRound() {
        IntIterator it = state.stopsTouchedByTransitCurrentRound();

        while (it.hasNext()) {
            final int fromStop = it.next();
            // no need to consider loop transfers, since we don't mark patterns here any more
            // loop transfers are already included by virtue of those stops having been reached
            state.transferToStops(fromStop, transitData.getTransfers(fromStop));
        }
        lifeCycle.transfersForRoundComplete();
    }

    /**
     * Create a trip search using {@link TripScheduleBoardSearch}.
     * <p/>
     * This is protected to allow reverse search to override and create a alight search instead.
     */
    private TripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
        if(!inFirstIteration && roundTracker.isFirstRound()) {
            // For the first round of every iteration(except the first) we restrict the first
            // departure to happen within the time-window of the iteration. Another way to put this,
            // is to say that we allow for the access leg to be time-shifted to a later departure,
            // but not past the previous iteration departure time. This save a bit of processing,
            // but most importantly allow us to use the departure-time as a pareto criteria in
            // time-table view. This is not valid for the first iteration, because we could jump on
            // a bus, take it on stop and walk back and then wait to board a later trip - this kind
            // of results would be rejected by earlier iterations, for all iterations except the
            // first.
            return calculator.createExactTripSearch(timeTable);
        }

        // Default: create a standard trip search
        return calculator.createTripSearch(timeTable);
    }

    // Track time spent, measure performance
    // TODO TGR - Replace by performance tests
    private AvgTimer timerRoute() { return timers.timerRoute(); }
    private AvgTimer timerByMinuteScheduleSearch() { return timers.timerByMinuteScheduleSearch(); }
    private AvgTimer timerByMinuteTransfers() { return timers.timerByMinuteTransfers(); }
}
