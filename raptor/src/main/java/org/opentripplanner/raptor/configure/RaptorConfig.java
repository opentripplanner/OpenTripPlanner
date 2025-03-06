package org.opentripplanner.raptor.configure;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorEnvironment;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.rangeraptor.ConcurrentCompositeRaptorRouter;
import org.opentripplanner.raptor.rangeraptor.DefaultRangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.RangeRaptor;
import org.opentripplanner.raptor.rangeraptor.RangeRaptorWorkerComposite;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.context.SearchContextViaLeg;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.PassThroughPointsService;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouter;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McStopArrivals;
import org.opentripplanner.raptor.rangeraptor.multicriteria.configure.McRangeRaptorConfig;
import org.opentripplanner.raptor.rangeraptor.standard.configure.StdRangeRaptorConfig;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorSearchWindowCalculator;
import org.opentripplanner.raptor.spi.ExtraMcRouterSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

/**
 * This class is responsible for creating a new search and holding application scoped Raptor state.
 * <p/>
 * This class should have APPLICATION scope. It keeps a reference to the environment and the
 * tuning parameters. The environment has a thread-pool, which should be APPLICATION scope.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorConfig<T extends RaptorTripSchedule> {

  private final RaptorEnvironment environment;
  private final RaptorTuningParameters tuningParameters;

  /** The service is not final, because it depends on the request. */
  private PassThroughPointsService passThroughPointsService = null;

  public RaptorConfig(RaptorTuningParameters tuningParameters, RaptorEnvironment environment) {
    this.tuningParameters = tuningParameters;
    this.environment = environment;
  }

  public static <T extends RaptorTripSchedule> RaptorConfig<T> defaultConfigForTest() {
    return new RaptorConfig<>(new RaptorTuningParameters() {}, new RaptorEnvironment() {});
  }

  public SearchContext<T> context(RaptorTransitDataProvider<T> transit, RaptorRequest<T> request) {
    // The passThroughPointsService is needed to create the context, so we initialize it here.
    this.passThroughPointsService = createPassThroughPointsService(request);
    var acceptC2AtDestination = passThroughPointsService.isNoop()
      ? null
      : passThroughPointsService.acceptC2AtDestination();
    return SearchContext.of(request, tuningParameters, transit, acceptC2AtDestination).build();
  }

  public RaptorRouter<T> createRangeRaptorWithStdWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request
  ) {
    var context = context(transitData, request);
    var stdConfig = new StdRangeRaptorConfig<>(context);
    var worker = createWorker(context.legs().getFirst(), stdConfig.state(), stdConfig.strategy());
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
    RangeRaptorWorker<T> worker = null;
    McStopArrivals<T> nextStopArrivals = null;

    if (request.searchParams().isVisitViaSearch()) {
      for (SearchContextViaLeg<T> cxLeg : context.legs().reversed()) {
        var c = new McRangeRaptorConfig<>(
          cxLeg,
          passThroughPointsService
        ).connectWithNextLegArrivals(nextStopArrivals);
        var w = createWorker(cxLeg, c.state(), c.strategy());
        worker = RangeRaptorWorkerComposite.of(w, worker);
        nextStopArrivals = c.stopArrivals();
      }
    } else {
      // The first leg is the only leg
      var leg = context.legs().getFirst();
      var c = new McRangeRaptorConfig<>(leg, passThroughPointsService).withHeuristics(heuristics);
      worker = createWorker(leg, c.state(), c.strategy());
    }
    return createRangeRaptor(context, worker);
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

  private static PassThroughPointsService createPassThroughPointsService(RaptorRequest<?> request) {
    return McRangeRaptorConfig.createPassThroughPointsService(
      request.searchParams().isPassThroughSearch(),
      request.searchParams().viaLocations()
    );
  }

  private RangeRaptorWorker<T> createWorker(
    SearchContextViaLeg<T> ctxLeg,
    RaptorWorkerState<T> workerState,
    RoutingStrategy<T> routingStrategy
  ) {
    var ctx = ctxLeg.parent();
    return new DefaultRangeRaptorWorker<>(
      workerState,
      routingStrategy,
      ctx.transitData(),
      ctx.slackProvider(),
      ctxLeg.accessPaths(),
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
      ctx.legs().getFirst().accessPaths(),
      ctx.roundTracker(),
      ctx.calculator(),
      ctx.createLifeCyclePublisher(),
      ctx.performanceTimers(),
      environment.timeoutHook()
    );
  }
}
