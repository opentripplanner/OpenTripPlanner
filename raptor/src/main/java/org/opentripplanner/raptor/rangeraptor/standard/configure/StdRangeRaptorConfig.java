package org.opentripplanner.raptor.rangeraptor.standard.configure;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.raptor.rangeraptor.path.PathParetoSetComparators.paretoComparator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.path.configure.PathConfig;
import org.opentripplanner.raptor.rangeraptor.standard.ArrivalTimeRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.standard.MinTravelDurationRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.standard.StdRangeRaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.standard.StdWorkerState;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimesOnlyStopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.SimpleArrivedAtDestinationCheck;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.SimpleBestNumberOfTransfers;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.UnknownPathFactory;
import org.opentripplanner.raptor.rangeraptor.standard.debug.DebugStopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.standard.heuristics.HeuristicsAdapter;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.ArrivedAtDestinationCheck;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.BestNumberOfTransfers;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.StopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StdStopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StdStopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.path.EgressArrivalToPathAdapter;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;

/**
 * The responsibility of this class is to wire different standard range raptor worker configurations
 * together based on the context passed into the class. There is a factory (create) method for each
 * legal configuration.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class StdRangeRaptorConfig<T extends RaptorTripSchedule> {

  private final SearchContext<T> ctx;
  private final PathConfig<T> pathConfig;
  private final RoutingStrategy<T> strategy;
  private final Set<Class<?>> oneOfInstanceTypes = new HashSet<>();

  private RaptorWorkerState<T> state;
  private BestTimes bestTimes;
  private StdStopArrivals<T> stopArrivals;
  private ArrivedAtDestinationCheck arrivedAtDestinationCheck;
  private BestNumberOfTransfers bestNumberOfTransfers;

  public StdRangeRaptorConfig(SearchContext<T> context) {
    new VerifyRequestIsValid(context).verify();
    this.ctx = context;
    this.pathConfig = new PathConfig<>(context);
    this.strategy = createWorkerStrategy();
  }

  public RaptorWorkerState<T> state() {
    return resolveState();
  }

  public RoutingStrategy<T> strategy() {
    return strategy;
  }

  public Heuristics createHeuristics(RaptorRouterResult<T> results) {
    return oneOf(
      new HeuristicsAdapter(
        ctx.nStops(),
        egressPaths(),
        ctx.calculator(),
        ctx.costCalculator(),
        results.extractBestOverallArrivals(),
        results.extractBestTransitArrivals(),
        results.extractBestNumberOfTransfers()
      ),
      Heuristics.class
    );
  }

  /* private factory methods */

  private RoutingStrategy<T> createWorkerStrategy() {
    return switch (ctx.profile()) {
      case STANDARD, BEST_TIME -> new ArrivalTimeRoutingStrategy<>(
        resolveState(),
        ctx.createTimeBasedBoardingSupport(),
        ctx.calculator()
      );
      case MIN_TRAVEL_DURATION -> new MinTravelDurationRoutingStrategy<>(
        resolveState(),
        ctx.createTimeBasedBoardingSupport(),
        ctx.calculator(),
        ctx.lifeCycle()
      );
      case MULTI_CRITERIA -> throw new IllegalArgumentException(ctx.profile().toString());
    };
  }

  private StdRangeRaptorWorkerState<T> resolveState() {
    if (state == null) {
      this.state = oneOf(
        new StdRangeRaptorWorkerState<>(
          ctx.calculator(),
          resolveBestTimes(),
          createStopArrivals(),
          resolveBestNumberOfTransfers(),
          resolveArrivedAtDestinationCheck()
        ),
        StdWorkerState.class
      );
    }
    return (StdRangeRaptorWorkerState<T>) state;
  }

  /**
   *  Cache best times; request scope
   */
  private BestTimes resolveBestTimes() {
    if (bestTimes == null) {
      bestTimes = new BestTimes(ctx.nStops(), ctx.calculator(), ctx.lifeCycle());
    }
    return bestTimes;
  }

  private StopArrivalsState<T> createStopArrivals() {
    return switch (ctx.profile()) {
      case STANDARD -> stdStopArrivalsState();
      case BEST_TIME, MIN_TRAVEL_DURATION -> createBestTimeStopArrivalsState();
      case MULTI_CRITERIA -> throw new IllegalArgumentException(ctx.profile().toString());
    };
  }

  private StopArrivalsState<T> createBestTimeStopArrivalsState() {
    return oneOf(
      new BestTimesOnlyStopArrivalsState<>(
        resolveBestTimes(),
        createSimpleBestNumberOfTransfers(),
        unknownPathFactory()
      ),
      StopArrivalsState.class
    );
  }

  /**
   * Create a Standard Range Raptor state for the given context. If debugging is enabled, the stop
   * arrival state is wrapped.
   */
  private StopArrivalsState<T> stdStopArrivalsState() {
    var state = oneOf(
      new StdStopArrivalsState<>(resolveStopArrivals(), destinationArrivalPaths()),
      StopArrivalsState.class
    );
    return wrapStopArrivalsStateWithDebugger(state);
  }

  private StopArrivalsState<T> wrapStopArrivalsStateWithDebugger(StopArrivalsState<T> state) {
    if (ctx.debugFactory().isDebugStopArrival()) {
      return new DebugStopArrivalsState<>(
        ctx.lifeCycle(),
        ctx.debugFactory(),
        stopsCursor(),
        state
      );
    } else {
      return state;
    }
  }

  private DestinationArrivalPaths<T> destinationArrivalPaths() {
    var destinationArrivalPaths = pathConfig.createDestArrivalPathsStdSearch();

    // Add egressArrivals to stops and bind them to the destination arrival paths. The
    // adapter notify the destination on each new egress stop arrival.
    var pathsAdapter = createEgressArrivalToPathAdapter(destinationArrivalPaths);

    resolveStopArrivals().setupEgressStopStates(egressPaths(), pathsAdapter);

    return destinationArrivalPaths;
  }

  private EgressArrivalToPathAdapter<T> createEgressArrivalToPathAdapter(
    DestinationArrivalPaths<T> destinationArrivalPaths
  ) {
    return withArrivedAtDestinationCheck(
      new EgressArrivalToPathAdapter<>(
        destinationArrivalPaths,
        ctx.calculator(),
        ctx.slackProvider(),
        stopsCursor(),
        ctx.lifeCycle()
      )
    );
  }

  private ArrivedAtDestinationCheck resolveArrivedAtDestinationCheck() {
    if (arrivedAtDestinationCheck == null) {
      // Default to simple version
      withArrivedAtDestinationCheck(createSimpleArrivedAtDestinationCheck());
    }
    return arrivedAtDestinationCheck;
  }

  /**
   * Always create new cursors - it has state local to the caller
   */
  private StopsCursor<T> stopsCursor() {
    return new StopsCursor<>(resolveStopArrivals(), ctx.calculator(), ctx.boardSlackProvider());
  }

  private StdStopArrivals<T> resolveStopArrivals() {
    if (stopArrivals == null) {
      this.stopArrivals = withBestNumberOfTransfers(
        oneOf(
          new StdStopArrivals<T>(ctx.nRounds(), ctx.nStops(), ctx.lifeCycle()),
          StdStopArrivals.class
        )
      );
    }
    return stopArrivals;
  }

  /**
   * Return instance if created by heuristics or null if not needed.
   */
  private SimpleBestNumberOfTransfers createSimpleBestNumberOfTransfers() {
    return withBestNumberOfTransfers(
      new SimpleBestNumberOfTransfers(ctx.nStops(), ctx.lifeCycle())
    );
  }

  private BestNumberOfTransfers resolveBestNumberOfTransfers() {
    if (bestNumberOfTransfers == null) {
      withBestNumberOfTransfers(createSimpleBestNumberOfTransfers());
    }
    return bestNumberOfTransfers;
  }

  private UnknownPathFactory<T> unknownPathFactory() {
    return new UnknownPathFactory<>(
      resolveBestTimes(),
      resolveBestNumberOfTransfers(),
      ctx.calculator(),
      ctx.slackProvider().transferSlack(),
      egressPaths(),
      MIN_TRAVEL_DURATION.is(ctx.profile()),
      paretoComparator(ctx.paretoSetTimeConfig(), ParetoSetCost.NONE, null, null),
      ctx.lifeCycle()
    );
  }

  private SimpleArrivedAtDestinationCheck createSimpleArrivedAtDestinationCheck() {
    return new SimpleArrivedAtDestinationCheck(
      resolveBestTimes(),
      egressPaths().egressesWitchStartByWalking(),
      egressPaths().egressesWitchStartByARide()
    );
  }

  private EgressPaths egressPaths() {
    return Objects.requireNonNull(
      ctx.legs().getLast().egressPaths(),
      "Last leg must have non-null egressPaths"
    );
  }

  private <S extends BestNumberOfTransfers> S withBestNumberOfTransfers(S value) {
    this.bestNumberOfTransfers = oneOf(value, BestNumberOfTransfers.class);
    return value;
  }

  private <S extends ArrivedAtDestinationCheck> S withArrivedAtDestinationCheck(S value) {
    this.arrivedAtDestinationCheck = oneOf(value, ArrivedAtDestinationCheck.class);
    return value;
  }

  /**
   * Verify only one {@code instance} of the given types exist, all types are request scoped. A
   * class may implement more than one role(interface); Hence a list of types, not just one type
   * argument. Skip types for the {@code instance} where the instance just wrap another instance
   * delegating all calls to it.
   */
  private <V> V oneOf(V instance, Class<?>... types) {
    for (Class<?> type : types) {
      if (!type.isInstance(instance)) {
        throw new IllegalArgumentException(
          "The instance of type " +
          instance.getClass().getSimpleName() +
          " is not an instance of type " +
          type.getSimpleName() +
          "."
        );
      }
      if (oneOfInstanceTypes.contains(type)) {
        throw new IllegalStateException(
          "An instance for is already initialized for type: " + type.getSimpleName()
        );
      }
      oneOfInstanceTypes.add(type);
    }
    return instance;
  }
}
