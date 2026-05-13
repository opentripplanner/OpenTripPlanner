package org.opentripplanner.warmup;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.updater.GraphUpdaterStatus;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.warmup.api.WarmupParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs GraphQL trip queries in a background thread during OTP startup to warm up the
 * application before production traffic arrives.
 * <p>
 * The worker sends sequential queries through the configured GraphQL API (TransModel or GTFS),
 * exercising the full stack: GraphQL parsing, data fetchers, routing (Raptor + A*), itinerary
 * filtering, and response serialization. This warms up JIT compilation, GraphQL schema caches,
 * routing data structures, and other lazily initialized components. It alternates between
 * depart-at / arrive-by and cycles through access/egress modes (walk, bike, car-to-park).
 * <p>
 * It starts after Raptor transit data is created and stops when the health probe
 * reports "UP" (all updaters primed).
 */
class WarmupWorker implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(WarmupWorker.class);
  private static final int MAX_QUERIES = 100;

  private final WarmupParameters parameters;
  private final WarmupQueryStrategy queryStrategy;
  private final Supplier<GraphUpdaterStatus> updaterStatusProvider;

  WarmupWorker(
    WarmupParameters parameters,
    OtpServerRequestContext serverContext,
    Supplier<GraphUpdaterStatus> updaterStatusProvider
  ) {
    this.parameters = parameters;
    this.updaterStatusProvider = updaterStatusProvider;
    this.queryStrategy = switch (parameters.api()) {
      case TRANSMODEL -> new TransmodelWarmupQueryExecutor(
        serverContext,
        parameters.accessModes(),
        parameters.egressModes()
      );
      case GTFS -> new GtfsWarmupQueryExecutor(
        serverContext,
        parameters.accessModes(),
        parameters.egressModes()
      );
    };
  }

  @Override
  public void run() {
    LOG.info(
      "Application warmup started. Sending {} GraphQL trip queries from {} to {}.",
      parameters.api(),
      parameters.from(),
      parameters.to()
    );

    var startTime = Instant.now();
    int queryCount = 0;
    int failureCount = 0;

    try {
      while (queryCount < MAX_QUERIES) {
        if (isHealthy()) {
          LOG.info(
            "Application warmup complete: {} queries ({} failures) in {}. All updaters primed.",
            queryCount,
            failureCount,
            DurationUtils.durationToStr(Duration.between(startTime, Instant.now()))
          );
          return;
        }

        queryCount++;
        boolean arriveBy = queryCount % 2 == 0;
        if (!executeQuery(queryCount, arriveBy)) {
          failureCount++;
        }
      }

      LOG.info(
        "Application warmup reached maximum of {} queries ({} failures) in {}" +
          " before all updaters were primed.",
        MAX_QUERIES,
        failureCount,
        DurationUtils.durationToStr(Duration.between(startTime, Instant.now()))
      );
    } catch (Throwable e) {
      LOG.error("Application warmup terminated by error after {} queries.", queryCount, e);
    }
  }

  /** @return true if the query succeeded without errors, false otherwise. */
  private boolean executeQuery(int queryCount, boolean arriveBy) {
    var queryStart = Instant.now();
    try {
      boolean success = queryStrategy.execute(
        parameters.from(),
        parameters.to(),
        arriveBy,
        queryCount
      );
      var elapsed = Duration.between(queryStart, Instant.now());
      LOG.info(
        "Warmup query #{} completed in {}.",
        queryCount,
        DurationUtils.durationToStr(elapsed)
      );
      return success;
    } catch (Exception e) {
      var elapsed = Duration.between(queryStart, Instant.now());
      LOG.info(
        "Warmup query #{} failed in {}: {}",
        queryCount,
        DurationUtils.durationToStr(elapsed),
        e.getMessage()
      );
      LOG.debug("Warmup query #{} exception detail", queryCount, e);
      return false;
    }
  }

  private boolean isHealthy() {
    var status = updaterStatusProvider.get();
    return status == null || status.listUnprimedUpdaters().isEmpty();
  }
}
