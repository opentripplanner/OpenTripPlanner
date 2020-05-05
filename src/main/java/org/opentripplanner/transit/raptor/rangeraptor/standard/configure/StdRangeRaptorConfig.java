package org.opentripplanner.transit.raptor.rangeraptor.standard.configure;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.path.configure.PathConfig;
import org.opentripplanner.transit.raptor.rangeraptor.standard.ArrivedAtDestinationCheck;
import org.opentripplanner.transit.raptor.rangeraptor.standard.BestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.standard.NoWaitTransitWorker;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StdRangeRaptorWorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StdTransitWorker;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StdWorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimesOnlyStopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.SimpleArrivedAtDestinationCheck;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.SimpleBestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.standard.debug.DebugStopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics.HeuristicSearch;
import org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics.HeuristicsAdapter;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.StdStopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.Stops;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.path.EgressArrivalToPathAdapter;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;

import java.util.function.BiFunction;


/**
 * The responsibility of this class is to wire different standard range raptor
 * worker configurations together based on the context passed into the class.
 * There is a factory (create) method for each legal configuration.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class StdRangeRaptorConfig<T extends RaptorTripSchedule> {

    private final SearchContext<T> ctx;
    private final PathConfig<T> pathConfig;

    private boolean workerCreated = false;
    private BestTimes bestTimes = null;
    private Stops<T> stops = null;
    private ArrivedAtDestinationCheck destinationCheck = null;
    private BestNumberOfTransfers bestNumberOfTransfers = null;


    public StdRangeRaptorConfig(SearchContext<T> context) {
        this.ctx = context;
        this.pathConfig = new PathConfig<>(context);
    }

    /**
     * Create a heuristic search using the provided callback to create the worker.
     * The callback is necessary because the heuristics MUST be created before the
     * worker, if not the heuristic can not be added to the worker lifecycle and fails.
     */
    public HeuristicSearch<T> createHeuristicSearch(
            BiFunction<WorkerState<T>, RoutingStrategy<T>, Worker<T>> createWorker
    ) {
        StdRangeRaptorWorkerState<T> state = createState();
        Heuristics heuristics = createHeuristicsAdapter();
        return new HeuristicSearch<>(createWorker.apply(state, createWorkerStrategy(state)), heuristics);
    }

    public Worker<T> createSearch(
            BiFunction<WorkerState<T>, RoutingStrategy<T>, Worker<T>> createWorker
    ) {
        StdRangeRaptorWorkerState<T> state = createState();
        return createWorker.apply(state, createWorkerStrategy(state));
    }


    /* private factory methods */

    private StdRangeRaptorWorkerState<T> createState() {
        new VerifyRequestIsValid(ctx).verify();
        switch (ctx.profile()) {
            case STANDARD:
            case NO_WAIT_STD:
                return workerState(stdStopArrivalsState());
            case BEST_TIME:
            case NO_WAIT_BEST_TIME:
                return workerState(bestTimeStopArrivalsState());
        }
        throw new IllegalArgumentException(ctx.profile().toString());
    }

    private RoutingStrategy<T> createWorkerStrategy(StdWorkerState<T> state) {
        switch (ctx.profile()) {
            case STANDARD:
            case BEST_TIME:
                return new StdTransitWorker<>(state, ctx.slackProvider(), ctx.calculator());
            case NO_WAIT_STD:
            case NO_WAIT_BEST_TIME:
                return new NoWaitTransitWorker<>(state, ctx.slackProvider(), ctx.calculator());
        }
        throw new IllegalArgumentException(ctx.profile().toString());
    }

    private Heuristics createHeuristicsAdapter() {
        assertNotNull(bestNumberOfTransfers);
        return new HeuristicsAdapter(
                bestTimes(),
                this.bestNumberOfTransfers,
                ctx.egressLegs(),
                ctx.calculator(),
                ctx.lifeCycle()
        );
    }

    private StdRangeRaptorWorkerState<T> workerState(StopArrivalsState<T> stopArrivalsState) {
        return new StdRangeRaptorWorkerState<>(ctx.calculator(), bestTimes(), stopArrivalsState, destinationCheck());
    }

    private BestTimesOnlyStopArrivalsState<T> bestTimeStopArrivalsState() {
        return new BestTimesOnlyStopArrivalsState<>(bestTimes(), simpleBestNumberOfTransfers());
    }

    /**
     * Return instance if created by heuristics or null if not needed.
     */
    private SimpleBestNumberOfTransfers simpleBestNumberOfTransfers() {
        SimpleBestNumberOfTransfers value = new SimpleBestNumberOfTransfers(
                ctx.nStops(),
                ctx.roundProvider()
        );
        setBestNumberOfTransfers(value);
        return value;
    }

    /**
     * Create a Standard Range Raptor state for the given context. If debugging is enabled,
     * the stop arrival state is wrapped.
     */
    private StopArrivalsState<T> stdStopArrivalsState() {
        StdStopArrivalsState<T> state = new StdStopArrivalsState<>(stops(), destinationArrivalPaths());
        return wrapStopArrivalsStateWithDebugger(state);
    }

    private StopArrivalsState<T> wrapStopArrivalsStateWithDebugger(StopArrivalsState<T> state) {
        if (ctx.debugFactory().isDebugStopArrival()) {
            return new DebugStopArrivalsState<>(ctx.roundProvider(), ctx.debugFactory(), stopsCursor(), state);
        } else {
            return state;
        }
    }

    private Stops<T> stops() {
        if (stops == null) {
            stops = new Stops<>(
                    ctx.nRounds(),
                    ctx.nStops(),
                    ctx.roundProvider()
            );
            setBestNumberOfTransfers(stops);
        }
        return stops;
    }

    private void setBestNumberOfTransfers(BestNumberOfTransfers bestNumberOfTransfers) {
        assertSetValueIsNull("bestNumberOfTransfers", this.bestNumberOfTransfers, bestNumberOfTransfers);
        this.bestNumberOfTransfers = bestNumberOfTransfers;
    }

    private StopsCursor<T> stopsCursor() {
        // Always create new cursors
        return new StopsCursor<>(stops(), ctx.calculator(), ctx.boardSlackProvider());
    }

    private DestinationArrivalPaths<T> destinationArrivalPaths() {
        DestinationArrivalPaths<T> destinationArrivalPaths = pathConfig.createDestArrivalPaths(false);

        // Add egressArrivals to stops and bind them to the destination arrival paths. The
        // adapter notify the destination on each new egress stop arrival.
        EgressArrivalToPathAdapter<T> pathsAdapter = new EgressArrivalToPathAdapter<>(
                destinationArrivalPaths,
                ctx.calculator(),
                stopsCursor(),
                ctx.lifeCycle(),
                ctx.debugFactory()
        );

        // Use the  adapter to play the role of the destination arrival check
        setDestinationCheck(pathsAdapter);

        stops().setupEgressStopStates(ctx.egressLegs(), pathsAdapter::add);

        return destinationArrivalPaths;
    }

    private BestTimes bestTimes() {
        // Cache best times; request scope
        if (bestTimes == null) {
            bestTimes = new BestTimes(ctx.nStops(), ctx.calculator(), ctx.lifeCycle());
        }
        return bestTimes;
    }

    private ArrivedAtDestinationCheck destinationCheck() {
        // Cache best times; request scope
        if (destinationCheck == null) {
            setDestinationCheck(simpleDestinationCheck());
        }
        return destinationCheck;
    }

    private void setDestinationCheck(ArrivedAtDestinationCheck check) {
        // Cache best times; request scope
        if (destinationCheck != null) {
            throw new IllegalStateException(
                    "ArrivedAtDestinationCheck is alredy initialized: " + destinationCheck.getClass().getSimpleName()
            );
        }
        destinationCheck = check;
    }

    private SimpleArrivedAtDestinationCheck simpleDestinationCheck() {
        return new SimpleArrivedAtDestinationCheck(ctx.egressStops(), bestTimes());
    }

    /**
     * This assert should only be called when creating a worker is the next step
     */
    private void assertOnlyOneWorkerIsCreated() {
        if (workerCreated) {
            throw new IllegalStateException(
                    "Create a new config for each worker. Do not reuse the config instance, " +
                            "this may lead to unpredictable behavior."
            );
        }
        workerCreated = true;
    }

    private void assertSetValueIsNull(String name, Object setValue, Object newValue) {
        if (setValue != null) {
            throw new IllegalStateException(
                    "There is more than one instance of " + name + ": " +
                            newValue.getClass().getSimpleName() + ", " +
                            setValue.getClass().getSimpleName()
            );
        }
    }

    private void assertNotNull(Object value) {
        if(value == null) {
            throw new NullPointerException();
        }
    }
}
