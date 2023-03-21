package org.opentripplanner.raptor.rangeraptor.multicriteria.configure;

import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.MultiCriteriaRequest;
import org.opentripplanner.raptor.api.request.RaptorTransitPriorityGroupCalculator;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McRangeRaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McStopArrivals;
import org.opentripplanner.raptor.rangeraptor.multicriteria.MultiCriteriaRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.ArrivalParetoSetComparatorFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2.StopArrivalFactoryC2;
import org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2.PatternRideC2;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2.TransitPriorityGroupRideFactory;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.path.configure.PathConfig;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * Configure and create multi-criteria worker, state and child classes.
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
    return includeC2()
      ? createTransitWorkerStrategy(
        state,
        createPatternRideC2Factory(),
        PatternRideC2.paretoComparatorRelativeCost(dominanceFunctionC2())
      )
      : createTransitWorkerStrategy(
        state,
        PatternRideC1.factory(),
        PatternRideC1.paretoComparatorRelativeCost()
      );
  }

  private <R extends PatternRide<T>> RoutingStrategy<T> createTransitWorkerStrategy(
    McRangeRaptorWorkerState<T> state,
    PatternRideFactory<T, R> factory,
    ParetoComparator<R> patternRideComparator
  ) {
    return new MultiCriteriaRoutingStrategy<>(
      state,
      context.createTimeBasedBoardingSupport(),
      factory,
      context.costCalculator(),
      context.slackProvider(),
      createPatternRideParetoSet(patternRideComparator)
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
    return includeC2() ? new StopArrivalFactoryC2<>() : new StopArrivalFactoryC1<>();
  }

  private McStopArrivals<T> createStopArrivals() {
    return new McStopArrivals<>(
      context.nStops(),
      context.egressPaths(),
      context.accessPaths(),
      createDestinationArrivalPaths(),
      createFactoryParetoComparator(),
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

  private <R extends PatternRide<T>> ParetoSet<R> createPatternRideParetoSet(
    ParetoComparator<R> comparator
  ) {
    return new ParetoSet<>(comparator, context.debugFactory().paretoSetPatternRideListener());
  }

  private DestinationArrivalPaths<T> createDestinationArrivalPaths() {
    if (paths == null) {
      paths = pathConfig.createDestArrivalPathsWithGeneralizedCost();
    }
    return paths;
  }

  private ArrivalParetoSetComparatorFactory<McStopArrival<T>> createFactoryParetoComparator() {
    return ArrivalParetoSetComparatorFactory.factory(mcRequest().relaxC1(), dominanceFunctionC2());
  }

  private MultiCriteriaRequest<T> mcRequest() {
    return context.multiCriteria();
  }

  /**
   * Currently "transit-priority-groups" is the only feature using two multi-criteria(c2).
   */
  private boolean includeC2() {
    return mcRequest().transitPriorityCalculator().isPresent();
  }

  private PatternRideFactory<T, PatternRideC2<T>> createPatternRideC2Factory() {
    return new TransitPriorityGroupRideFactory<>(getTransitPriorityGroupCalculator());
  }

  @Nullable
  private DominanceFunction dominanceFunctionC2() {
    // transit-priority-groups is the only feature using two multi-criteria(c2).
    return mcRequest()
      .transitPriorityCalculator()
      .map(RaptorTransitPriorityGroupCalculator::dominanceFunction)
      .orElse(null);
  }

  @Nullable
  private RaptorTransitPriorityGroupCalculator getTransitPriorityGroupCalculator() {
    return mcRequest().transitPriorityCalculator().orElse(null);
  }
}
