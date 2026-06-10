package org.opentripplanner.raptor.configure;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.request.RaptorEnvironment;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.extensions.extrasearch.ExtraMcRouterSearch;
import org.opentripplanner.raptor.rangeraptor.ConcurrentCompositeRaptorRouter;
import org.opentripplanner.raptor.rangeraptor.DefaultRangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.RangeRaptor;
import org.opentripplanner.raptor.rangeraptor.RangeRaptorWorkerComposite;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.context.SearchContextViaSegments;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouter;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McRangeRaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.configure.McRangeRaptorConfig;
import org.opentripplanner.raptor.rangeraptor.standard.configure.StdRangeRaptorConfig;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorSearchWindowCalculator;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * This class is responsible for creating a new search and holding application-scoped Raptor state.
 * <p/>
 * This class should have APPLICATION scope. It keeps a reference to the environment and the
 * tuning parameters. The environment has a thread-pool, which should be APPLICATION scope.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorConfig<T extends RaptorTripSchedule> {

  private final RaptorEnvironment environment;
  private final RaptorTuningParameters tuningParameters;

  public RaptorConfig(RaptorTuningParameters tuningParameters, RaptorEnvironment environment) {
    this.tuningParameters = tuningParameters;
    this.environment = environment;
  }

  public SearchContext<T> context(RaptorTransitDataProvider<T> transit, RaptorRequest<T> request) {
    return SearchContext.of(request, tuningParameters, transit).build();
  }

  public RaptorRouter<T> createRangeRaptorWithStdWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request
  ) {
    var context = context(transitData, request);
    var stdConfig = new StdRangeRaptorConfig<>(context);
    var worker = createWorker(
      context.segments().getFirst(),
      stdConfig.state(),
      stdConfig.strategy()
    );
    return createRangeRaptor(context, worker);
  }

  public RaptorRouter<T> createRangeRaptorWithMcWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request,
    Heuristics heuristics,
    @Nullable ExtraMcRouterSearch<T> extraMcSearch
  ) {
    var mainSearch = createRangeRaptorWithMcWorker(transitData, request, heuristics);

    if (extraMcSearch == null) {
      return mainSearch;
    }
    var alternativeSearch = createRangeRaptorWithMcWorker(
      extraMcSearch.createTransitDataAlternativeSearch(transitData),
      request,
      heuristics
    );
    return new ConcurrentCompositeRaptorRouter<>(
      mainSearch,
      alternativeSearch,
      extraMcSearch.merger(),
      threadPool(),
      environment::mapInterruptedException
    );
  }

  private RaptorRouter<T> createRangeRaptorWithMcWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request,
    Heuristics heuristics
  ) {
    var context = context(transitData, request);
    RangeRaptorWorker<T> nextWorker = null;
    McRangeRaptorWorkerState<T> nextWorkerState = null;

    if (request.searchParams().isViaSearch()) {
      // Note! We start with the last segment to be able to link the segments together
      for (SearchContextViaSegments<T> ctxSegment : context.segments().reversed()) {
        var c = new McRangeRaptorConfig<>(ctxSegment).connectWithNextSegmentState(nextWorkerState);
        var s = c.state();
        var w = createWorker(ctxSegment, s, c.strategy());
        nextWorker = RangeRaptorWorkerComposite.of(c.createPathParetoComparator(), w, nextWorker);
        nextWorkerState = s;
      }
    } else {
      // The first segment is the only segment
      var segment = context.segments().getFirst();
      var c = new McRangeRaptorConfig<>(segment).withHeuristics(heuristics);
      nextWorker = createWorker(segment, c.state(), c.strategy());
    }
    return createRangeRaptor(context, nextWorker);
  }

  public RaptorRouter<T> createRangeRaptorWithHeuristicSearch(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request
  ) {
    return createRangeRaptorWithStdWorker(transitData, request);
  }

  public Heuristics createHeuristic(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request,
    RaptorRouterResult<T> results
  ) {
    var context = context(transitData, request);
    return new StdRangeRaptorConfig<>(context).createHeuristics(results);
  }

  public boolean isMultiThreaded() {
    return threadPool() != null;
  }

  @Nullable
  public ExecutorService threadPool() {
    return environment.threadPool();
  }

  public void shutdown() {
    if (threadPool() != null) {
      threadPool().shutdown();
    }
  }

  public RuntimeException mapInterruptedException(InterruptedException e) {
    return environment.mapInterruptedException(e);
  }

  public RaptorSearchWindowCalculator searchWindowCalculator() {
    return new RaptorSearchWindowCalculator(tuningParameters.dynamicSearchWindowCoefficients());
  }

  /* private factory methods */

  private RangeRaptorWorker<T> createWorker(
    SearchContextViaSegments<T> ctxSegment,
    RaptorWorkerState<T> workerState,
    RoutingStrategy<T> routingStrategy
  ) {
    var ctx = ctxSegment.parent();
    return new DefaultRangeRaptorWorker<>(
      workerState,
      routingStrategy,
      ctx.transitData(),
      ctx.slackProvider(),
      ctxSegment.accessPaths(),
      ctx.calculator(),
      ctx.lifeCycle(),
      ctx.performanceTimers(),
      ctx.useConstrainedTransfers()
    );
  }

  private RaptorRouter<T> createRangeRaptor(SearchContext<T> ctx, RangeRaptorWorker<T> worker) {
    return new RangeRaptor<>(
      worker,
      ctx.transitData(),
      ctx.segments().getFirst().accessPaths(),
      ctx.roundTracker(),
      ctx.calculator(),
      ctx.createLifeCyclePublisher(),
      ctx.performanceTimers(),
      environment.timeoutHook()
    );
  }
}
