package org.opentripplanner.transit.raptor.rangeraptor;

import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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


/**
 * The algorithm used herein is described in
 * <p>
 * Conway, Matthew Wigginton, Andrew Byrd, and Marco van der Linden. “Evidence-Based Transit and
 * Land Use Sketch Planning
 * Using Interactive Accessibility Methods on Combined Schedule and Headway-Based Networks.”
 * Transportation Research Record 2653 (2017). doi:10.3141/2653-06.
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
 * This version do NOT support the following features:
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
     * The RangeRaptor state - we delegate keeping track of state to the state object,
     * this allow the worker implementation to focus on the algorithm, while
     * the state keep track of the result.
     * <p/>
     * This also allow us to try out different strategies for storing the result in memory.
     * For a long time we had a state witch stored all data as int arrays in addition to the
     * current object-oriented approach. There were no performance differences(=> GC is not
     * the bottle neck), so we dropped the integer array implementation.
     */
    private final WorkerState<T> state;

    /**
     * The round tracker keep track for the current Raptor round, and abort the search if the
     * round max limit is reached.
     */
    private final RoundTracker roundTracker;

    private final RaptorTransitDataProvider<T> transitData;

    private final TransitCalculator calculator;

    private final WorkerPerformanceTimers timers;

    private final Map<Integer, List<RaptorTransfer>> accessArrivedByWalking;

    private final Map<Integer, List<RaptorTransfer>> accessArrivedOnBoard;

    private final LifeCycleEventPublisher lifeCycle;

    private final int mimNumberOfRounds;

    private boolean inFirstIteration = true;

    private int iterationDepartureTime;


    public RangeRaptorWorker(
            WorkerState<T> state,
            RoutingStrategy<T> transitWorker,
            RaptorTransitDataProvider<T> transitData,
            Collection<RaptorTransfer> accessPaths,
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
        this.accessArrivedByWalking = groupByRound(accessPaths, Predicate.not(RaptorTransfer::stopReachedOnBoard));
        this.accessArrivedOnBoard = groupByRound(accessPaths, RaptorTransfer::stopReachedOnBoard);
        this.mimNumberOfRounds = calculateMaxNumberOfRides(accessPaths);

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
                iterationDepartureTime = it.next();
                lifeCycle.setupIteration(iterationDepartureTime);
                runRaptorForMinute();
                inFirstIteration = false;
            }
        });
        return state.extractPaths();
    }

    /**
     * Perform one minute of a RAPTOR search.
     */
    private void runRaptorForMinute() {
        addAccessPaths(accessArrivedByWalking.get(0));

        while (hasMoreRounds()) {
            lifeCycle.prepareForNextRound(roundTracker.nextRound());

            // NB since we have transfer limiting not bothering to cut off search when there are no
            // more transfers as that will be rare and complicates the code
            findAllTransitForRound();

            addAccessPaths(accessArrivedOnBoard.get(round()));

            transfersForRound();

            lifeCycle.roundComplete(state.isDestinationReachedInCurrentRound());

            addAccessPaths(accessArrivedByWalking.get(round()));
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
        if(round() < mimNumberOfRounds) { return true; }
        return state.isNewRoundAvailable() && roundTracker.hasMoreRounds();
    }

    /**
     * Perform a scheduled search
     */
    private void findAllTransitForRound() {
        timerByMinuteScheduleSearch().time(() -> {
            IntIterator stops = state.stopsTouchedPreviousRound();
            Iterator<? extends RaptorRoute<T>> routeIterator = transitData.routeIterator(stops);

            while (routeIterator.hasNext()) {
                RaptorRoute<T> route = routeIterator.next();
                RaptorTripPattern pattern = route.pattern();
                TripScheduleSearch<T> tripSearch = createTripSearch(route.timetable());

                transitWorker.prepareForTransitWith(pattern, tripSearch);

                IntIterator stop = calculator.patternStopIterator(pattern.numberOfStopsInPattern());
                while (stop.hasNext()) {
                    transitWorker.routeTransitAtStop(stop.next());
                }
            }
            lifeCycle.transitsForRoundComplete();
        });
    }

    private void transfersForRound() {
        timerByMinuteTransfers().time(() -> {
            IntIterator it = state.stopsTouchedByTransitCurrentRound();

            while (it.hasNext()) {
                final int fromStop = it.next();
                // no need to consider loop transfers, since we don't mark patterns here any more
                // loop transfers are already included by virtue of those stops having been reached
                state.transferToStops(fromStop, transitData.getTransfers(fromStop));
            }

            lifeCycle.transfersForRoundComplete();
        });
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

    /**
     * Set the departure time in the scheduled search to the given departure time,
     * and prepare for the scheduled search at the next-earlier minute.
     */
    private void addAccessPaths(Collection<RaptorTransfer> accessPaths) {
        if(accessPaths == null) { return; }

        for (RaptorTransfer it : accessPaths) {
            // Earliest possible departure time from the origin, or latest possible arrival
            // time at the destination if searching backwards.
            int timeDependentDepartureTime = calculator.departureTime(it, iterationDepartureTime);

            // This access is not available after the iteration departure time
            if (timeDependentDepartureTime == -1) { continue; }

            transitWorker.setAccessToStop(it, iterationDepartureTime, timeDependentDepartureTime);
        }
    }

    private int round() {
        return roundTracker.round();
    }

    /**
     * Filter the given input keeping all elements satisfying the include predicate and return
     * a unmodifiable list.
     */
    private  static <T extends RaptorTransfer> Map<Integer, List<T>> groupByRound(
        Collection<T> input, Predicate<T> include
    ) {
        return input.stream()
            .filter(include)
            .collect(groupingBy(RaptorTransfer::numberOfRides));
    }

    private int calculateMaxNumberOfRides(Collection<RaptorTransfer> accessPaths) {
        return accessPaths.stream().mapToInt(RaptorTransfer::numberOfRides).max().orElse(0);
    }

    // Track time spent, measure performance
    // TODO TGR - Replace by performance tests
    private AvgTimer timerRoute() { return timers.timerRoute(); }
    private AvgTimer timerByMinuteScheduleSearch() { return timers.timerByMinuteScheduleSearch(); }
    private AvgTimer timerByMinuteTransfers() { return timers.timerByMinuteTransfers(); }
}
