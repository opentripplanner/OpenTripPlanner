package org.opentripplanner.updater;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is attached to the graph:
 *
 * <pre>
 * GraphUpdaterManager updaterManager = graph.getUpdaterManager();
 * </pre>
 * <p>
 * Each updater will run in its own thread. When changes to the graph have to be made by these
 * updaters, this should be done via the execute method of this manager to prevent race conditions
 * between graph write operations.
 */
public class GraphUpdaterManager implements WriteToGraphCallback, GraphUpdaterStatus {

  private static final Logger LOG = LoggerFactory.getLogger(GraphUpdaterManager.class);
  /**
   * This ensures a reasonable level of parallelism even for instances with a low CPU count.
   */
  private static final int MIN_POLLING_UPDATER_THREADS = 6;

  /**
   * OTP's multi-version concurrency control model for graph updating allows simultaneous reads, but
   * never simultaneous writes. We ensure this policy is respected by having a single writer thread,
   * which sequentially executes all graph updater tasks. Each task is a runnable that is scheduled
   * with the ExecutorService to run at regular intervals.
   * FIXME: In reality we're not using scheduleAtFixedInterval.
   *        We're scheduling for immediate execution from separate threads that sleep in a loop.
   *        We should perhaps switch to having polling GraphUpdaters call scheduleAtFixedInterval.
   */
  private final ScheduledExecutorService scheduler;

  private final ScheduledExecutorService pollingUpdaterPool;

  /**
   * A pool of threads on which the non-polling updaters will run. This creates a pool that will auto-scale up
   * to any size (maximum pool size is MAX_INT).
   */
  private final ExecutorService nonPollingUpdaterPool;

  /**
   * Keep track of all updaters so we can cleanly free resources associated with them at shutdown.
   */
  private final List<GraphUpdater> updaterList = new ArrayList<>();

  /**
   * The Graph that will be updated.
   */
  private final RealTimeUpdateContext realtimeUpdateContext;

  /**
   * Constructor.
   *
   */
  public GraphUpdaterManager(RealTimeUpdateContext context, List<GraphUpdater> updaters) {
    this.realtimeUpdateContext = context;
    // Thread factories used to create new threads, giving them more human-readable names.
    var graphWriterThreadFactory = new ThreadFactoryBuilder().setNameFormat("graph-writer").build();
    this.scheduler = Executors.newSingleThreadScheduledExecutor(graphWriterThreadFactory);
    var updaterThreadFactory = new ThreadFactoryBuilder().setNameFormat("updater-%d").build();
    this.pollingUpdaterPool = Executors.newScheduledThreadPool(
      Math.max(MIN_POLLING_UPDATER_THREADS, Runtime.getRuntime().availableProcessors()),
      updaterThreadFactory
    );
    this.nonPollingUpdaterPool = Executors.newCachedThreadPool(updaterThreadFactory);

    for (GraphUpdater updater : updaters) {
      updaterList.add(updater);
      updater.setup(this);
    }
  }

  /**
   * This should be called only once at startup to kick off every updater in its own thread, and
   * only after all the updaters have had their setup methods called.
   */
  public void startUpdaters() {
    for (GraphUpdater updater : updaterList) {
      Runnable runUpdater = () -> {
        try {
          updater.run();
        } catch (Exception e) {
          LOG.error("Error while running updater {}:", updater.getClass().getName(), e);
        }
      };
      if (updater instanceof PollingGraphUpdater pollingGraphUpdater) {
        LOG.info("Scheduling polling updater {}", updater);
        if (pollingGraphUpdater.runOnlyOnce()) {
          pollingUpdaterPool.schedule(runUpdater, 0, TimeUnit.SECONDS);
        } else {
          pollingUpdaterPool.scheduleWithFixedDelay(
            runUpdater,
            0,
            pollingGraphUpdater.pollingPeriod().toSeconds(),
            TimeUnit.SECONDS
          );
        }
      } else {
        LOG.info("Starting new thread for updater {}", updater);
        nonPollingUpdaterPool.execute(runUpdater);
      }
    }
    reportReadinessForUpdaters();
  }

  /**
   * Initiate the graceful shutdown of thread pools.
   * Running tasks will be cancelled.
   * Pending tasks will be ignored.
   */
  public void stop() {
    stop(true);
  }

  /**
   * Initiate the graceful shutdown of thread pools.
   * Optionally wait for running tasks to be processed before stopping (useful in tests).
   * Pending tasks will be ignored.
   */
  public void stop(boolean cancelRunningTasks) {
    // TODO: find a better way to stop these threads
    LOG.info("Stopping updater manager with {} updaters.", numberOfUpdaters());
    // Shutdown updaters
    if (cancelRunningTasks) {
      pollingUpdaterPool.shutdownNow();
      nonPollingUpdaterPool.shutdownNow();
    } else {
      pollingUpdaterPool.shutdown();
      nonPollingUpdaterPool.shutdown();
    }

    try {
      boolean ok =
        pollingUpdaterPool.awaitTermination(15, TimeUnit.SECONDS) &&
        nonPollingUpdaterPool.awaitTermination(15, TimeUnit.SECONDS);
      if (!ok) {
        LOG.warn("Timeout waiting for updaters to finish.");
      }
    } catch (InterruptedException e) {
      // This should not happen
      LOG.warn("Interrupted while waiting for updaters to finish.");
    }

    // Clean up updaters
    for (GraphUpdater updater : updaterList) {
      updater.teardown();
    }
    updaterList.clear();

    // Shutdown scheduler
    scheduler.shutdownNow();
    try {
      boolean ok = scheduler.awaitTermination(30, TimeUnit.SECONDS);
      if (!ok) {
        LOG.warn("Timeout waiting for scheduled task to finish.");
      }
    } catch (InterruptedException e) {
      // This should not happen
      LOG.warn("Interrupted while waiting for scheduled task to finish.");
    }
    LOG.info("Stopped updater manager");
  }

  @Override
  public Future<?> execute(GraphWriterRunnable runnable) {
    return scheduler.submit(() -> {
      try {
        runnable.run(realtimeUpdateContext);
      } catch (Exception e) {
        LOG.error("Error while running graph writer {}:", runnable.getClass().getName(), e);
      }
    });
  }

  @Override
  public int numberOfUpdaters() {
    return updaterList.size();
  }

  /**
   * Return the number of updaters started, but not ready.
   *
   * @see GraphUpdater#isPrimed()
   */
  @Override
  public List<String> listUnprimedUpdaters() {
    return updaterList
      .stream()
      .filter(Predicate.not(GraphUpdater::isPrimed))
      .map(GraphUpdater::getConfigRef)
      .collect(Collectors.toList());
  }

  /**
   * Just an example of fetching status information from the graph updater manager to expose it in a
   * web service. More useful stuff should be added later.
   */
  @Override
  public Map<Integer, String> getUpdaterDescriptions() {
    Map<Integer, String> ret = new TreeMap<>();
    int i = 0;
    for (GraphUpdater updater : updaterList) {
      ret.put(i++, updater.toString());
    }
    return ret;
  }

  /**
   * Just an example of fetching status information from the graph updater manager to expose it in a
   * web service. More useful stuff should be added later.
   */
  public GraphUpdater getUpdater(int id) {
    if (id >= updaterList.size()) {
      return null;
    }
    return updaterList.get(id);
  }

  public Class<?> getUpdaterClass(int id) {
    GraphUpdater updater = getUpdater(id);
    return updater == null ? null : updater.getClass();
  }

  public List<GraphUpdater> getUpdaterList() {
    return updaterList;
  }

  public ExecutorService getPollingUpdaterPool() {
    return pollingUpdaterPool;
  }

  public ExecutorService getNonPollingUpdaterPool() {
    return nonPollingUpdaterPool;
  }

  public ScheduledExecutorService getScheduler() {
    return scheduler;
  }

  /**
   * This method start a task during startup and log a message when all updaters are initialized.
   * When all updaters are ready, then OTP is ready for processing routing requests.
   * <p>
   * It starts its own thread using busy-wait(anti-pattern). The ideal would be to add a callback
   * from each updater to notify the manager about 'isPrimed'. But, this is simple, the thread is
   * mostly idle, and it is short-lived, so the busy-wait is a compromise.
   */
  private void reportReadinessForUpdaters() {
    Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("updater-ready").build()
    ).submit(() -> {
      boolean otpIsShuttingDown = false;

      while (!otpIsShuttingDown) {
        try {
          if (updaterList.stream().allMatch(GraphUpdater::isPrimed)) {
            LOG.info(
              "OTP UPDATERS INITIALIZED ({} updaters) - OTP {} is ready for routing!",
              updaterList.size(),
              OtpProjectInfo.projectInfo().version
            );
            return;
          }
          //noinspection BusyWait
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          otpIsShuttingDown = true;
          LOG.info("OTP is shutting down, cancelling wait for updaters readiness.");
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        }
      }
    });
  }
}
