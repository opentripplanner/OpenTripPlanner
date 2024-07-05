package org.opentripplanner.raptor.rangeraptor.multicriteria.configure;

import java.util.Objects;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.MultiCriteriaRequest;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupCalculator;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.PassThroughPointsService;
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
import org.opentripplanner.raptor.rangeraptor.multicriteria.passthrough.BitSetPassThroughPointsService;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2.PassThroughRideFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2.PatternRideC2;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2.TransitGroupPriorityRideFactory;
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
  private PassThroughPointsService passThroughPointsService;

  public McRangeRaptorConfig(
    SearchContext<T> context,
    PassThroughPointsService passThroughPointsService
  ) {
    this.context = Objects.requireNonNull(context);
    this.passThroughPointsService = Objects.requireNonNull(passThroughPointsService);
    this.pathConfig = new PathConfig<>(context);
  }

  /**
   * The PassThroughPointsService is injected into the transit-calculator, so it needs to be
   * created before the context(which create the calculator).So, to be able to do this, this
   * factory is static, and the service is passed back in when this config is instantiated.
   */
  public static PassThroughPointsService passThroughPointsService(
    MultiCriteriaRequest<?> multiCriteriaRequest
  ) {
    return BitSetPassThroughPointsService.of(multiCriteriaRequest.passThroughPoints());
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
      passThroughPointsService,
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
      var c2Comp = includeC2() ? dominanceFunctionC2() : null;
      paths = pathConfig.createDestArrivalPaths(resolveCostConfig(), c2Comp);
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
   * Use c2 in the search, this is use-case specific. For example the pass-through or
   * transit-group-priority features uses the c2 value.
   */
  private boolean includeC2() {
    return mcRequest().includeC2();
  }

  private PatternRideFactory<T, PatternRideC2<T>> createPatternRideC2Factory() {
    if (isPassThrough()) {
      return new PassThroughRideFactory<>(passThroughPointsService);
    }
    if (isTransitPriority()) {
      return new TransitGroupPriorityRideFactory<>(getTransitGroupPriorityCalculator());
    }
    throw new IllegalStateException("Only pass-through and transit-priority uses c2.");
  }

  @Nullable
  private DominanceFunction dominanceFunctionC2() {
    if (isPassThrough()) {
      return passThroughPointsService.dominanceFunction();
    }
    if (isTransitPriority()) {
      return getTransitGroupPriorityCalculator().dominanceFunction();
    }
    return null;
  }

  private RaptorTransitGroupCalculator getTransitGroupPriorityCalculator() {
    return mcRequest().transitPriorityCalculator().orElseThrow();
  }

  private boolean isPassThrough() {
    return mcRequest().hasPassThroughPoints();
  }

  private boolean isTransitPriority() {
    return mcRequest().transitPriorityCalculator().isPresent();
  }

  private ParetoSetCost resolveCostConfig() {
    if (isTransitPriority()) {
      return ParetoSetCost.USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
    }
    if (isPassThrough()) {
      return ParetoSetCost.USE_C1_AND_C2;
    }
    if (context.multiCriteria().relaxCostAtDestination() != null) {
      return ParetoSetCost.USE_C1_RELAX_DESTINATION;
    }
    return ParetoSetCost.USE_C1;
  }
}
