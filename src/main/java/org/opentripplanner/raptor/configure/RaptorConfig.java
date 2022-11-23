package org.opentripplanner.raptor.configure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.rangeraptor.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.HeuristicSearch;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.Worker;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.configure.McRangeRaptorConfig;
import org.opentripplanner.raptor.rangeraptor.standard.configure.StdRangeRaptorConfig;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorSearchWindowCalculator;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * This class is responsible for creating a new search and holding application scoped Raptor state.
 * <p/>
 * This class should have APPLICATION scope. It manage a threadPool, and hold a reference to the
 * application tuning parameters.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorConfig<T extends RaptorTripSchedule> {

  private final ExecutorService threadPool;
  private final RaptorTuningParameters tuningParameters;

  public RaptorConfig(RaptorTuningParameters tuningParameters) {
    this.tuningParameters = tuningParameters;
    this.threadPool = createNewThreadPool(tuningParameters.searchThreadPoolSize());
  }

  public static <T extends RaptorTripSchedule> RaptorConfig<T> defaultConfigForTest() {
    return new RaptorConfig<>(new RaptorTuningParameters() {});
  }

  public SearchContext<T> context(RaptorTransitDataProvider<T> transit, RaptorRequest<T> request) {
    return new SearchContext<>(request, tuningParameters, transit);
  }

  public Worker<T> createStdWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request
  ) {
    SearchContext<T> context = context(transitData, request);
    return new StdRangeRaptorConfig<>(context).createSearch((s, w) -> createWorker(context, s, w));
  }

  public Worker<T> createMcWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request,
    Heuristics heuristics
  ) {
    final SearchContext<T> context = context(transitData, request);
    return new McRangeRaptorConfig<>(context)
      .createWorker(heuristics, (s, w) -> createWorker(context, s, w));
  }

  public HeuristicSearch<T> createHeuristicSearch(
    RaptorTransitDataProvider<T> transitData,
    CostCalculator<T> costCalculator,
    RaptorRequest<T> request
  ) {
    SearchContext<T> context = context(transitData, request);
    return new StdRangeRaptorConfig<>(context)
      .createHeuristicSearch((s, w) -> createWorker(context, s, w), costCalculator);
  }

  public boolean isMultiThreaded() {
    return threadPool != null;
  }

  public ExecutorService threadPool() {
    return threadPool;
  }

  public void shutdown() {
    if (threadPool != null) {
      threadPool.shutdown();
    }
  }

  public RaptorSearchWindowCalculator searchWindowCalculator() {
    return new RaptorSearchWindowCalculator(tuningParameters.dynamicSearchWindowCoefficients());
  }

  /* private factory methods */

  private Worker<T> createWorker(
    SearchContext<T> ctx,
    WorkerState<T> workerState,
    RoutingStrategy<T> routingStrategy
  ) {
    return new RangeRaptorWorker<>(
      workerState,
      routingStrategy,
      ctx.transit(),
      ctx.slackProvider(),
      ctx.accessPaths(),
      ctx.roundProvider(),
      ctx.calculator(),
      ctx.createLifeCyclePublisher(),
      ctx.performanceTimers(),
      ctx.enableConstrainedTransfers()
    );
  }

  @Nullable
  private ExecutorService createNewThreadPool(int size) {
    return size > 0 ? Executors.newFixedThreadPool(size) : null;
  }
}
