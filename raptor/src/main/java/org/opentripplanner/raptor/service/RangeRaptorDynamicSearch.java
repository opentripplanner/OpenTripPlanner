package org.opentripplanner.raptor.service;

import static org.opentripplanner.raptor.api.model.SearchDirection.FORWARD;
import static org.opentripplanner.raptor.api.model.SearchDirection.REVERSE;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.service.HeuristicToRunResolver.resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.api.request.SearchParamsBuilder;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouter;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorSearchWindowCalculator;
import org.opentripplanner.raptor.spi.ExtraMcRouterSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This search helps the {@link RaptorService} to configure
 * heuristics and set dynamic search parameters like EDT, LAT and raptor-search-window.
 * <p>
 * If possible the forward and reverse heuristics will be run in parallel.
 * <p>
 * Depending on which optimization is enabled and which search parameters are set a forward and/or a
 * reverse "single-iteration" raptor search is performed and heuristics are collected. This is used
 * to configure the "main" multi-iteration RangeRaptor search.
 */
public class RangeRaptorDynamicSearch<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(RangeRaptorDynamicSearch.class);

  private final RaptorConfig<T> config;
  private final RaptorTransitDataProvider<T> transitData;
  private final RaptorRequest<T> originalRequest;
  private final RaptorSearchWindowCalculator dynamicSearchWindowCalculator;

  @Nullable
  private final ExtraMcRouterSearch<T> extraMcSearch;

  private final HeuristicSearchTask<T> fwdHeuristics;
  private final HeuristicSearchTask<T> revHeuristics;

  public RangeRaptorDynamicSearch(
    RaptorConfig<T> config,
    RaptorTransitDataProvider<T> transitData,
    @Nullable ExtraMcRouterSearch<T> extraMcSearch,
    RaptorRequest<T> originalRequest
  ) {
    this.config = config;
    this.transitData = transitData;
    this.originalRequest = originalRequest;
    this.dynamicSearchWindowCalculator = config
      .searchWindowCalculator()
      .withSearchParams(originalRequest.searchParams());
    this.extraMcSearch = extraMcSearch;

    this.fwdHeuristics = new HeuristicSearchTask<>(FORWARD, "Forward", config, transitData);
    this.revHeuristics = new HeuristicSearchTask<>(REVERSE, "Reverse", config, transitData);
  }

  public RaptorResponse<T> route() {
    try {
      enableHeuristicSearchBasedOnOptimizationsAndSearchParameters();

      // Run the heuristics if no destination is reached
      runHeuristics();

      // Set search-window and other dynamic calculated parameters
      var dynamicRequest = requestWithDynamicSearchParams(originalRequest);

      return createAndRunDynamicRRWorker(dynamicRequest);
    } catch (DestinationNotReachedException e) {
      return new RaptorResponse<>(
        Collections.emptyList(),
        null,
        // If a trip exists(forward heuristics succeed), but is outside the calculated
        // search-window, then set the search-window params as if the request was
        // performed. This enables the client to page to the next window
        requestWithDynamicSearchParams(originalRequest),
        false
      );
    }
  }

  /**
   * Only exposed for testing purposes
   */
  @Nullable
  public Heuristics getDestinationHeuristics() {
    if (!originalRequest.useDestinationPruning()) {
      return null;
    }
    LOG.debug("RangeRaptor - Destination pruning enabled.");
    return revHeuristics.result();
  }

  /**
   * Create and prepare heuristic search (both FORWARD and REVERSE) based on optimizations and input
   * search parameters. This is done for Standard and Multi-criteria profiles only.
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
   * Run standard "singe-iteration" raptor search to calculate heuristics - this should be really
   * fast to run compared with a (multi-criteria) range-raptor search.
   *
   * @throws DestinationNotReachedException if destination is not reached.
   */
  private void runHeuristics() {
    if (isItPossibleToRunHeuristicsInParallel()) {
      runHeuristicsInParallel();
    } else {
      runHeuristicsSequentially();
    }
    fwdHeuristics.debugCompareResult(revHeuristics);
  }

  private RaptorResponse<T> createAndRunDynamicRRWorker(RaptorRequest<T> request) {
    LOG.debug("Main request: {}", request);
    RaptorRouter<T> raptorRouter;

    // Create worker
    if (request.profile().is(MULTI_CRITERIA)) {
      raptorRouter = config.createRangeRaptorWithMcWorker(
        transitData,
        request,
        getDestinationHeuristics(),
        extraMcSearch
      );
    } else {
      raptorRouter = config.createRangeRaptorWithStdWorker(transitData, request);
    }

    // Route
    var result = raptorRouter.route();

    // create and return response
    return new RaptorResponse<>(
      result.extractPaths(),
      new DefaultStopArrivals(result),
      request,
      // This method is not run unless the heuristic reached the destination
      true
    );
  }

  private boolean isItPossibleToRunHeuristicsInParallel() {
    SearchParams s = originalRequest.searchParams();
    return (
      config.isMultiThreaded() &&
      originalRequest.runInParallel() &&
      s.isEarliestDepartureTimeSet() &&
      s.isLatestArrivalTimeSet() &&
      fwdHeuristics.isEnabled() &&
      revHeuristics.isEnabled()
    );
  }

  /**
   * @throws DestinationNotReachedException if destination is not reached
   */
  private void runHeuristicsInParallel() {
    fwdHeuristics.withRequest(originalRequest);
    revHeuristics.withRequest(originalRequest);
    Future<?> asyncResult = null;
    try {
      asyncResult = config.threadPool().submit(fwdHeuristics::run);
      revHeuristics.run();
      asyncResult.get();
      LOG.debug(
        "Route using RangeRaptor - " + "REVERSE and FORWARD heuristic search performed in parallel."
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // propagate interruption to the running task.
      asyncResult.cancel(true);
      throw config.mapInterruptedException(e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof DestinationNotReachedException dnr) {
        throw dnr;
      }
      LOG.error(e.getMessage() + ". Request: " + originalRequest, e);
      throw new IllegalStateException(
        "Failed to run FORWARD/REVERSE heuristic search in parallel. Details: " + e.getMessage()
      );
    }
  }

  /**
   * @throws DestinationNotReachedException if destination is not reached
   */
  private void runHeuristicsSequentially() {
    List<HeuristicSearchTask<T>> tasks = listTasksInOrder();

    if (tasks.isEmpty()) {
      return;
    }

    // Run the first heuristic search
    Heuristics result = runHeuristicSearchTask(tasks.get(0));
    calculateDynamicSearchParametersFromHeuristics(result);

    if (tasks.size() == 1) {
      return;
    }

    // Run the second heuristic search
    runHeuristicSearchTask(tasks.get(1));
  }

  private Heuristics runHeuristicSearchTask(HeuristicSearchTask<T> task) {
    RaptorRequest<T> request = task.getDirection().isForward()
      ? requestForForwardHeurSearchWithDynamicSearchParams()
      : requestForReverseHeurSearchWithDynamicSearchParams();

    task.withRequest(request).run();

    return task.result();
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
    return originalRequest
      .mutate()
      .searchParams()
      .earliestDepartureTime(transitData.getValidTransitDataStartTime())
      .build();
  }

  private RaptorRequest<T> requestForReverseHeurSearchWithDynamicSearchParams() {
    if (originalRequest.searchParams().isLatestArrivalTimeSet()) {
      return originalRequest;
    }
    return originalRequest
      .mutate()
      .searchParams()
      .latestArrivalTime(
        transitData.getValidTransitDataEndTime() +
        originalRequest.searchParams().accessEgressMaxDurationSeconds()
      )
      .build();
  }

  private RaptorRequest<T> requestWithDynamicSearchParams(RaptorRequest<T> request) {
    SearchParamsBuilder<T> builder = request.mutate().searchParams();

    if (!request.searchParams().isEarliestDepartureTimeSet()) {
      builder.earliestDepartureTime(dynamicSearchWindowCalculator.getEarliestDepartureTime());
    }
    if (!request.searchParams().isSearchWindowSet()) {
      builder.searchWindowInSeconds(dynamicSearchWindowCalculator.getSearchWindowSeconds());
    }
    // We do not set the latest-arrival-time, because we do not want to limit the forward
    // multi-criteria search, it does not have much effect on the performance - we only risk
    // loosing optimal results.
    return builder.build();
  }

  private void calculateDynamicSearchParametersFromHeuristics(@Nullable Heuristics heuristics) {
    if (heuristics != null) {
      dynamicSearchWindowCalculator
        .withHeuristics(
          heuristics.bestOverallJourneyTravelDuration(),
          heuristics.minWaitTimeForJourneysReachingDestination()
        )
        .calculate();
    }
  }
}
