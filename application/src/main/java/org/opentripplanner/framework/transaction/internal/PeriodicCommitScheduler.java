package org.opentripplanner.framework.transaction.internal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PeriodicCommitScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(PeriodicCommitScheduler.class);

  private final String name;
  private final ScheduledExecutorService scheduler;
  private final Supplier<Future<Void>> performCommit;
  private final long interval_ms;
  private long warningLimit_ms;

  PeriodicCommitScheduler(
    String name,
    Duration commitInterval,
    ThreadFactory threadFactory,
    Supplier<Future<Void>> performCommit
  ) {
    this.name = name;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
    this.performCommit = performCommit;
    this.interval_ms = commitInterval.toMillis();
    this.warningLimit_ms = interval_ms;
    this.scheduler.scheduleAtFixedRate(this::runCommit, interval_ms, interval_ms, MILLISECONDS);
  }

  void shutdown() {
    scheduler.shutdown();
  }

  private void runCommit() {
    long startTime = System.currentTimeMillis();
    var f = performCommit.get();
    try {
      f.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      LOG.error("Error during periodic commit", e);
    }
    long elapsedTime = System.currentTimeMillis() - startTime;
    if (elapsedTime > warningLimit_ms) {
      if (warningLimit_ms == interval_ms) {
        // We increase the limit after the first log event to:
        // - avoid spamming the logs if the update is exceeding the limit by a small amount.
        // - distinguish between the first (initialization) and the followups.
        warningLimit_ms *= 2;
        LOG.warn(
          "Commit is taking a long time to complete (including queued wait), {} ms for {}. " +
            "The update interval is {} ms. This is the first warning, increasing the warning limit " +
            "to {} ms.",
          elapsedTime,
          name,
          interval_ms,
          warningLimit_ms
        );
      } else {
        LOG.warn(
          "Commit is taking a long time to complete (including queued wait), {} ms for {}. " +
            "The warning limit is {} ms. If this continues to happen, consider investigating why or " +
            "increasing the update interval.",
          elapsedTime,
          name,
          warningLimit_ms
        );
      }
    }
  }
}
