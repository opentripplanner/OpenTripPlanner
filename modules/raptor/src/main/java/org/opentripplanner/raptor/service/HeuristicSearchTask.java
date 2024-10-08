package org.opentripplanner.raptor.service;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;

import javax.annotation.Nullable;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper around a {@link RaptorWorker} to allow for some small additional features. This
 * is mostly to extracted some "glue" out of the {@link RangeRaptorDynamicSearch} to make that
 * simpler and let it focus on the main bossiness logic.
 * <p>
 * This class is not meant for reuse, create one task for each potential heuristic search. The task
 * must be {@link #enable()}d before it is {@link #run()}.
 */
public class HeuristicSearchTask<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(HeuristicSearchTask.class);

  private final SearchDirection direction;
  private final String name;
  private final RaptorConfig<T> config;
  private final RaptorTransitDataProvider<T> transitData;

  private boolean run = false;
  private RaptorWorker<T> search = null;
  private RaptorRequest<T> originalRequest;
  private RaptorRequest<T> heuristicRequest;
  private RaptorWorkerResult<T> result = null;

  public HeuristicSearchTask(
    RaptorRequest<T> request,
    RaptorConfig<T> config,
    RaptorTransitDataProvider<T> transitData
  ) {
    this(request.searchDirection(), request.alias(), config, transitData);
    this.originalRequest = request;
  }

  public HeuristicSearchTask(
    SearchDirection direction,
    String name,
    RaptorConfig<T> config,
    RaptorTransitDataProvider<T> transitData
  ) {
    this.direction = direction;
    this.name = name;
    this.config = config;
    this.transitData = transitData;
  }

  public String name() {
    return name;
  }

  public void enable() {
    this.run = true;
  }

  public boolean isEnabled() {
    return run;
  }

  public SearchDirection getDirection() {
    return direction;
  }

  @Nullable
  public Heuristics result() {
    if (result == null) {
      return null;
    }
    return config.createHeuristic(transitData, heuristicRequest, result);
  }

  public HeuristicSearchTask<T> withRequest(RaptorRequest<T> request) {
    this.originalRequest = request;
    return this;
  }

  public void forceRun() {
    enable();
    run();
  }

  public void debugCompareResult(HeuristicSearchTask<T> other) {
    if (!isEnabled() || !other.isEnabled()) {
      return;
    }
    DebugHeuristics.debug(name(), result(), other.name(), other.result(), originalRequest);
  }

  /**
   * @throws DestinationNotReachedException if destination is not reached
   */
  void run() {
    if (!run) {
      return;
    }

    long start = System.currentTimeMillis();

    createHeuristicSearchIfNotExist(originalRequest);

    LOG.debug("Heuristic search: {}", heuristicRequest);
    this.result = search.route();
    LOG.debug("Heuristic result: {}", result);

    if (!result.isDestinationReached()) {
      throw new DestinationNotReachedException();
    }
    if (LOG.isDebugEnabled()) {
      String time = DurationUtils.msToSecondsStr(System.currentTimeMillis() - start);
      LOG.debug("RangeRaptor - {} heuristic search performed in {}.", name, time);
    }
  }

  private void createHeuristicSearchIfNotExist(RaptorRequest<T> request) {
    if (search == null) {
      var profile = MIN_TRAVEL_DURATION;

      var builder = request
        .mutate()
        // Disable any optimization that is not valid for a heuristic search
        .clearOptimizations()
        .profile(profile)
        .searchDirection(direction);

      builder.searchParams().searchOneIterationOnly();

      // Add this last, it depends on generating an alias from the set values
      builder.performanceTimers(
        request.performanceTimers().withNamePrefix(builder.generateAlias())
      );

      heuristicRequest = builder.build();
      search = config.createHeuristicSearch(transitData, heuristicRequest);
    }
  }
}
