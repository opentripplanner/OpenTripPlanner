package org.opentripplanner.raptor.rangeraptor.multicriteria.configure;

import java.util.function.BiFunction;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.Worker;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McRangeRaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McStopArrivals;
import org.opentripplanner.raptor.rangeraptor.multicriteria.MultiCriteriaRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.path.configure.PathConfig;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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

  private McStopArrivals<T> createStopArrivals() {
    return new McStopArrivals<>(
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
