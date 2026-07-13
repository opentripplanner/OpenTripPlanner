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
  private long warningLimitMillis;

  PeriodicCommitScheduler(
    String name,
    Duration commitInterval,
    ThreadFactory threadFactory,
    Supplier<Future<Void>> performCommit
  ) {
    this.name = name;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
    this.performCommit = performCommit;
    var intervalMillis = commitInterval.toMillis();
    this.warningLimitMillis = intervalMillis;
    scheduler.scheduleAtFixedRate(this::runCommit, intervalMillis, intervalMillis, MILLISECONDS);
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
    if (elapsedTime > warningLimitMillis) {
      warningLimitMillis *= 2;
      LOG.warn(
        "Commit is taking a long time to complete (including queued wait), {} ms for {}.",
        elapsedTime,
        name
      );
    }
  }
}
