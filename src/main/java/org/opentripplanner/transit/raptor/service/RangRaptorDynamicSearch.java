package org.opentripplanner.transit.raptor.service;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.FORWARD;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.REVERSE;
import static org.opentripplanner.transit.raptor.service.HeuristicToRunResolver.resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters;


/**
 * This search help the {@link org.opentripplanner.transit.raptor.RaptorService} to configure
 * heuristics and set dynamic search parameters like EDT, LAT and raptor-search-window.
 * <p>
 * If possible the forward and revers heuristics will be run in parallel.
 * <p>
 * Depending on witch optimization is enabled and witch search parameters is set a forward and/or a
 * revers "singel-iteration" raptor search is performed and heuristics is collected. This is used
 * to configure the "main" multi-iteration RangeRaptor search.
 */
public class RangRaptorDynamicSearch<T extends RaptorTripSchedule> {
    private static final Logger LOG = LoggerFactory.getLogger(RangRaptorDynamicSearch.class);

    private final RaptorConfig<T> config;
    private final RaptorTransitDataProvider<T> transitData;
    private final RaptorRequest<T> originalRequest;
    private final RaptorSearchWindowCalculator dynamicSearchParamsCalculator;

    private HeuristicSearchTask<T> fwdHeuristics;
    private HeuristicSearchTask<T> revHeuristics;

    public RangRaptorDynamicSearch(
            RaptorConfig<T> config,
            RaptorTransitDataProvider<T> transitData,
            RaptorRequest<T> originalRequest
    ) {
        this.config = config;
        this.transitData = transitData;
        this.originalRequest = originalRequest;
        this.dynamicSearchParamsCalculator = config.searchWindowCalculator()
                .withSearchParams(originalRequest.searchParams());

        this.fwdHeuristics = new HeuristicSearchTask<>(FORWARD, "Forward", config, transitData);
        this.revHeuristics = new HeuristicSearchTask<>(REVERSE, "Reverse", config, transitData);
    }

    public RaptorResponse<T> route() {
        try {
            enableHeuristicSearchBasedOnOptimizationsAndSearchParameters();

            // Run heuristics, if no destination is reached
           runHeuristics();

            RaptorRequest<T> mcRequest = originalRequest;
            mcRequest = mcRequestWithDynamicSearchParams(mcRequest);

            return createAndRunWorker(mcRequest);
        }
        catch (DestinationNotReachedException e) {
            return new RaptorResponse<>(
                    Collections.emptyList(),
                    originalRequest,
                    originalRequest
            );
        }
    }

    /**
     * Create and prepare heuristic search (both FORWARD and REVERSE) based on optimizations and
     * input search parameters. This is done for Standard and Multi-criteria profiles only.
     */
    private void enableHeuristicSearchBasedOnOptimizationsAndSearchParameters() {
        // We delegate this to a static method to be able to write unit test on this logic
        resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters(
                originalRequest,
                fwdHeuristics::enable,
                revHeuristics::enable
        );
    }

    /**
     * Run standard "singe-iteration" raptor search to calculate heuristics - this should be
     * really fast to run compared with a (multi-criteria) range-raptor search.
     *
     * @throws DestinationNotReachedException if destination is not reached.
     */
    private void runHeuristics() {
        if (isItPossibleToRunHeuristicsInParallel()) {
            runHeuristicsInParallel();
        }
        else {
            runHeuristicsSequentially();
        }
        fwdHeuristics.debugCompareResult(revHeuristics);
    }

    private RaptorResponse<T> createAndRunWorker(RaptorRequest<T> mcRequest) {

        LOG.debug("Raptor request: " + mcRequest.toString());
        Worker<T> worker;

        // Create worker
        if (mcRequest.profile().is(MULTI_CRITERIA)) {
            worker = config.createMcWorker(transitData, mcRequest, getDestinationHeuristics());
        }
        else {
            worker = config.createStdWorker(transitData, mcRequest);
        }

        // Route
        Collection<Path<T>> paths = worker.route();

        // create and return response
        return new RaptorResponse<>(paths, originalRequest, mcRequest);
    }

    private boolean isItPossibleToRunHeuristicsInParallel() {
        SearchParams s = originalRequest.searchParams();
        return config.isMultiThreaded()
                && originalRequest.runInParallel()
                && s.isEarliestDepartureTimeSet()
                && s.isLatestArrivalTimeSet()
                && fwdHeuristics.isEnabled()
                && revHeuristics.isEnabled();
    }

    /**
     * @throws DestinationNotReachedException if destination is not reached
     */
    private void runHeuristicsInParallel() {
        try {
            fwdHeuristics.withRequest(originalRequest);
            revHeuristics.withRequest(originalRequest);

            Future<?> f = config.threadPool().submit(fwdHeuristics::run);
            revHeuristics.run();
            f.get();
            LOG.debug("Route using RangeRaptor - "
                    + "REVERSE and FORWARD heuristic search performed in parallel.");
        }
        catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof DestinationNotReachedException) {
                throw new DestinationNotReachedException();
            }
            LOG.error(e.getMessage() + ". Request: " + originalRequest, e);
            throw new OtpAppException(
                    "Failed to run FORWARD/REVERSE heuristic search in parallel. Details: "
                            + e.getMessage());
        }
    }

    /**
     * @throws DestinationNotReachedException if destination is not reached
     */
    private void runHeuristicsSequentially() {
        List<HeuristicSearchTask<T>> tasks = listTasksInOrder();

        if(tasks.isEmpty()) { return; }

        // Run the first heuristic search
        HeuristicSearchTask<T> task;
        task = tasks.get(0);
        task.withRequest(originalRequest).run();
        calculateDynamicSearchParametersFromHeuristics(task.result());

        if(tasks.size() == 1) { return; }

        // Run the second heuristic search
        task = tasks.get(1);
        RaptorRequest<T> request = task.getDirection().isForward()
                ? requestForForwardHeurSearchWithDynamicSearchParams()
                : requestForReverseHeurSearchWithDynamicSearchParams();

        task.withRequest(request).run();
    }

    /**
     * If the earliest-departure-time(EDT) is set, the task order should be:
     * <ol>
     *     <li>{@code FORWARD}</li>
     *     <li>{@code REVERSE}</li>
     * </ol>
     * If not EDT is set, the latest-arrival-time is set, and the order should be the opposite,
     * with {@code REVERSE} first
     */
    private List<HeuristicSearchTask<T>> listTasksInOrder() {
        boolean performForwardFirst = originalRequest.searchParams().isEarliestDepartureTimeSet();

        List<HeuristicSearchTask<T>> list = performForwardFirst
                ? List.of(fwdHeuristics, revHeuristics)
                : List.of(revHeuristics, fwdHeuristics);

        return list.stream().filter(HeuristicSearchTask::isEnabled).collect(Collectors.toList());
    }

    private RaptorRequest<T> requestForForwardHeurSearchWithDynamicSearchParams() {
        if (originalRequest.searchParams().isEarliestDepartureTimeSet()) {
            return originalRequest;
        }
        return originalRequest.mutate().searchParams()
                .earliestDepartureTime(dynamicSearchParamsCalculator.getEarliestDepartureTime())
                .build();
    }

    private RaptorRequest<T> requestForReverseHeurSearchWithDynamicSearchParams() {
        if (originalRequest.searchParams().isLatestArrivalTimeSet()) {
            return originalRequest;
        }
        return originalRequest.mutate().searchParams()
                .latestArrivalTime(dynamicSearchParamsCalculator.getLatestArrivalTime())
                .build();
    }

    private RaptorRequest<T> mcRequestWithDynamicSearchParams(RaptorRequest<T> request) {

        SearchParamsBuilder<T> builder = request.mutate().searchParams();

        if (!request.searchParams().isEarliestDepartureTimeSet()) {
            builder.earliestDepartureTime(dynamicSearchParamsCalculator.getEarliestDepartureTime());
        }
        if (!request.searchParams().isSearchWindowSet()) {
            builder.searchWindowInSeconds(dynamicSearchParamsCalculator.getSearchWindowSeconds());
        }
        // We do not set the latest-arrival-time, because we do not want to limit the forward
        // multi-criteria search, it does not have much effect on the performance - we only risk
        // loosing optimal results.
        return builder.build();
    }

    private void calculateDynamicSearchParametersFromHeuristics(Heuristics heuristics) {
        if(heuristics != null) {
            dynamicSearchParamsCalculator
                    .withMinTripTime(heuristics.bestOverallJourneyTravelDuration())
                    .calculate();
        }
    }

    private Heuristics getDestinationHeuristics() {
        if (!originalRequest.useDestinationPruning()) { return null; }
        LOG.debug("RangeRaptor - Destination pruning enabled.");
        return revHeuristics.result();
    }
}
