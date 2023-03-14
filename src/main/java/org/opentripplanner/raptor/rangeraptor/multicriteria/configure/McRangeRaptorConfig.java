package org.opentripplanner.raptor.rangeraptor.multicriteria.configure;

import java.util.function.BiFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McRangeRaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McStopArrivals;
import org.opentripplanner.raptor.rangeraptor.multicriteria.MultiCriteriaRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.path.configure.PathConfig;

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
  public RaptorWorker<T> createWorker(
    Heuristics heuristics,
    BiFunction<RaptorWorkerState<T>, RoutingStrategy<T>, RaptorWorker<T>> createWorker
  ) {
    McRangeRaptorWorkerState<T> state = createState(heuristics);
    return createWorker.apply(state, createTransitWorkerStrategy(state));
  }

  /* private factory methods */

  private RoutingStrategy<T> createTransitWorkerStrategy(McRangeRaptorWorkerState<T> state) {
    return new MultiCriteriaRoutingStrategy<>(
      state,
      context.createTimeBasedBoardingSupport(),
      PatternRideC1.factory(),
      context.costCalculator(),
      context.slackProvider(),
      PatternRideC1.paretoComparatorRelativeCost(),
      context.debugFactory().paretoSetPatternRideListener()
    );
  }

  private McRangeRaptorWorkerState<T> createState(Heuristics heuristics) {
    return new McRangeRaptorWorkerState<>(
      createStopArrivals(),
      createDestinationArrivalPaths(),
      createHeuristicsProvider(heuristics),
      createStopArrivalFactory(),
      context.costCalculator(),
      context.calculator(),
      context.lifeCycle()
    );
  }

  private McStopArrivalFactory<T> createStopArrivalFactory() {
    // So fare there are no use-cases where the second criteria is used, so
    // we just return the C1 factory here. We do this to be able to benchmark
    // the performance. We will change this soon.
    return new StopArrivalFactoryC1<>();
  }

  private McStopArrivals<T> createStopArrivals() {
    return new McStopArrivals<>(
      context.nStops(),
      context.egressPaths(),
      context.accessPaths(),
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
      paths = pathConfig.createDestArrivalPathsWithGeneralizedCost();
    }
    return paths;
  }
}
