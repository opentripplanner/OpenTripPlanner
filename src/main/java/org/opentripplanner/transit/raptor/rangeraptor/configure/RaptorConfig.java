package org.opentripplanner.transit.raptor.rangeraptor.configure;

import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.RangeRaptorWorker;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.configure.McRangeRaptorConfig;
import org.opentripplanner.transit.raptor.rangeraptor.standard.configure.StdRangeRaptorConfig;
import org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics.HeuristicSearch;
import org.opentripplanner.transit.raptor.service.RaptorSearchWindowCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;
import org.opentripplanner.transit.raptor.service.WorkerPerformanceTimersCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * This class is responsible for creating a new search and holding
 * application scoped Raptor state.
 * <p/>
 * This class should have APPLICATION scope. It manage a threadPool,
 * and hold a reference to the application tuning parameters.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorConfig<T extends RaptorTripSchedule> {
    private final ExecutorService threadPool;
    private final RaptorTuningParameters tuningParameters;
    private final WorkerPerformanceTimersCache timers;


    public RaptorConfig(RaptorTuningParameters tuningParameters) {
        this.tuningParameters = tuningParameters;
        this.threadPool = createNewThreadPool(tuningParameters.searchThreadPoolSize());
        this.timers = new WorkerPerformanceTimersCache(isMultiThreaded());
    }

    public SearchContext<T> context(RaptorTransitDataProvider<T> transit, RaptorRequest<T> request) {
        return new SearchContext<>(request, tuningParameters, transit, timers.get(request));
    }

    public Worker<T> createStdWorker(RaptorTransitDataProvider<T> transitData, RaptorRequest<T> request) {
        SearchContext<T> context = context(transitData, request);
        return new StdRangeRaptorConfig<>(context).createSearch((s, w) -> createWorker(context, s, w));
    }

    public Worker<T> createMcWorker(RaptorTransitDataProvider<T> transitData, RaptorRequest<T> request, Heuristics heuristics) {
        final SearchContext<T> context = context(transitData, request);
        return new McRangeRaptorConfig<>(context).createWorker(heuristics, (s, w) -> createWorker(context, s, w));
    }

    public HeuristicSearch<T> createHeuristicSearch(
            RaptorTransitDataProvider<T> transitData,
            RaptorRequest<T> request
    ) {
        SearchContext<T> context = context(transitData, request);
        return new StdRangeRaptorConfig<>(context)
                .createHeuristicSearch((s, w) -> createWorker(context, s, w));
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
                ctx.accessLegs(),
                ctx.roundProvider(),
                ctx.calculator(),
                ctx.createLifeCyclePublisher(),
                ctx.timers()
        );
    }

    private ExecutorService createNewThreadPool(int size) {
        return size > 0 ? Executors.newFixedThreadPool(size) : null;
    }

}
