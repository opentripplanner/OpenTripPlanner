package org.opentripplanner.updater;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * Manages the lifecycle of all {@link GraphUpdater} instances: starts each updater on its own
 * thread, shuts them down cleanly, and tracks readiness.
 * <p>
 * Write tasks submitted by updaters are serialised by the {@link WriteToGraphCallback} passed at
 * construction — currently a {@link GraphWriterService}, which will be replaced by the new
 * {@link org.opentripplanner.framework.transaction.UpdateManager} framework.
 */
public class GraphUpdaterManager implements GraphUpdaterStatus {

  private static final Logger LOG = LoggerFactory.getLogger(GraphUpdaterManager.class);
  /**
   * This ensures a reasonable level of parallelism even for instances with a low CPU count.
   */
  private static final int MIN_POLLING_UPDATER_THREADS = 6;

  private final ScheduledExecutorService pollingUpdaterPool;

  /**
   * A pool of threads on which the non-polling updaters will run. This creates a pool that will
   * auto-scale up to any size (maximum pool size is MAX_INT).
   */
  private final ExecutorService nonPollingUpdaterPool;

  /**
   * Keep track of all updaters so we can cleanly free resources associated with them at shutdown.
   */
  private final List<GraphUpdater> updaterList = new ArrayList<>();

  private final Runnable shutdownGraphWriter;

  public GraphUpdaterManager(
    WriteToGraphCallback writeToGraphCallback,
    Runnable shutdownGraphWriter,
    List<GraphUpdater> updaters
  ) {
    var updaterThreadFactory = new ThreadFactoryBuilder().setNameFormat("updater-%d").build();
    this.pollingUpdaterPool = Executors.newScheduledThreadPool(
      Math.max(MIN_POLLING_UPDATER_THREADS, Runtime.getRuntime().availableProcessors()),
      updaterThreadFactory
    );
    this.nonPollingUpdaterPool = Executors.newCachedThreadPool(updaterThreadFactory);
    this.shutdownGraphWriter = shutdownGraphWriter;

    for (GraphUpdater updater : updaters) {
      updaterList.add(updater);
      updater.setup(writeToGraphCallback);
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
      shutdownGraphWriter.run();
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for updaters to finish.");
    }

    for (GraphUpdater updater : updaterList) {
      updater.teardown();
    }
    updaterList.clear();
    LOG.info("Stopped updater manager");
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

  @Override
  public Map<Integer, String> getUpdaterDescriptions() {
    Map<Integer, String> ret = new TreeMap<>();
    int i = 0;
    for (GraphUpdater updater : updaterList) {
      ret.put(i++, updater.toString());
    }
    return ret;
  }

  public GraphUpdater getUpdater(int id) {
    if (id >= updaterList.size()) {
      return null;
    }
    return updaterList.get(id);
  }

  @Override
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
