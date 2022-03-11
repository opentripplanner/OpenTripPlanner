package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.configure;

import java.util.function.BiFunction;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.McRangeRaptorWorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.MultiCriteriaRoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.StopArrivals;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.path.configure.PathConfig;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;


/**
 * Configure and create multicriteria worker, state and child classes.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class McRangeRaptorConfig<T extends RaptorTripSchedule> {
    private final SearchContext<T> context;
    private final PathConfig<T> pathConfig;

    private DestinationArrivalPaths<T> paths;

    public McRangeRaptorConfig(SearchContext<T> context) {
        this.context = context;
        this.pathConfig = new PathConfig<>(context);
    }

    /**
     * Create new multi-criteria worker with optional heuristics.
     */
    public Worker<T> createWorker(
            Heuristics heuristics,
            BiFunction<WorkerState<T>, RoutingStrategy<T>, Worker<T>> createWorker
    ) {
        McRangeRaptorWorkerState<T> state = createState(heuristics);
        return createWorker.apply(state, createTransitWorkerStrategy(state));
    }


    /* private factory methods */

    private RoutingStrategy<T> createTransitWorkerStrategy(McRangeRaptorWorkerState<T> state) {
        return new MultiCriteriaRoutingStrategy<>(
                state,
                context.slackProvider(),
                context.costCalculator(),
                context.debugFactory()
        );
    }

    private McRangeRaptorWorkerState<T> createState(Heuristics heuristics) {
        return new McRangeRaptorWorkerState<>(
                createStopArrivals(),
                createDestinationArrivalPaths(),
                createHeuristicsProvider(heuristics),
                context.costCalculator(),
                context.calculator(),
                context.lifeCycle()
        );
    }

    private StopArrivals<T> createStopArrivals() {
        return new StopArrivals<>(
                context.nStops(),
                context.egressPaths(),
                createDestinationArrivalPaths(),
                context.debugFactory()
        );
    }

    private HeuristicsProvider<T> createHeuristicsProvider(Heuristics heuristics) {
        if (heuristics == null) {
            return new HeuristicsProvider<>();
        } else {
            return new HeuristicsProvider<>(
                    heuristics,
                    context.roundProvider(),
                    createDestinationArrivalPaths(),
                    context.costCalculator(),
                    context.debugFactory()
            );
        }
    }

    private DestinationArrivalPaths<T> createDestinationArrivalPaths() {
        if (paths == null) {
            paths = pathConfig.createDestArrivalPaths(true);
        }
        return paths;
    }
}
