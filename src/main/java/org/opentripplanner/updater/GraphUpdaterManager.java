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
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;
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
   * OTP's multi-version concurrency control model for graph updating allows simultaneous reads, but
   * never simultaneous writes. We ensure this policy is respected by having a single writer thread,
   * which sequentially executes all graph updater tasks. Each task is a runnable that is scheduled
   * with the ExecutorService to run at regular intervals.
   * FIXME: In reality we're not using scheduleAtFixedInterval.
   *        We're scheduling for immediate execution from separate threads that sleep in a loop.
   *        We should perhaps switch to having polling GraphUpdaters call scheduleAtFixedInterval.
   */
  private final ScheduledExecutorService scheduler;

  /**
   * A pool of threads on which the updaters will run. This creates a pool that will auto-scale up
   * to any size (maximum pool size is MAX_INT).
   * FIXME The polling updaters occupy an entire thread, sleeping in between polling operations.
   */
  private final ExecutorService updaterPool;

  /**
   * Keep track of all updaters so we can cleanly free resources associated with them at shutdown.
   */
  private final List<GraphUpdater> updaterList = new ArrayList<>();

  /**
   * The Graph that will be updated.
   */
  private final Graph graph;
  private final TransitModel transitModel;

  /**
   * Constructor.
   *
   * @param transitModel is the Graph that will be updated.
   */
  public GraphUpdaterManager(Graph graph, TransitModel transitModel, List<GraphUpdater> updaters) {
    this.graph = graph;
    this.transitModel = transitModel;
    // Thread factory used to create new threads, giving them more human-readable names.
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("GraphUpdater-%d").build();
    this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
    this.updaterPool = Executors.newCachedThreadPool(threadFactory);

    for (GraphUpdater updater : updaters) {
      updaterList.add(updater);
      updater.setGraphUpdaterManager(this);
    }
  }

  /**
   * This should be called only once at startup to kick off every updater in its own thread, and
   * only after all the updaters have had their setup methods called.
   */
  public void startUpdaters() {
    for (GraphUpdater updater : updaterList) {
      LOG.info("Starting new thread for updater {}", updater.toString());
      updaterPool.execute(() -> {
        try {
          updater.run();
        } catch (Exception e) {
          LOG.error("Error while running updater {}:", updater.getClass().getName(), e);
        }
      });
    }
    reportReadinessForUpdaters();
  }

  public void stop() {
    // TODO: find a better way to stop these threads

    // Shutdown updaters
    updaterPool.shutdownNow();
    try {
      boolean ok = updaterPool.awaitTermination(30, TimeUnit.SECONDS);
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
  }

  @Override
  public Future<?> execute(GraphWriterRunnable runnable) {
    return scheduler.submit(() -> {
      try {
        runnable.run(graph, transitModel);
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

  public ExecutorService getUpdaterPool() {
    return updaterPool;
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
    Executors
      .newSingleThreadExecutor()
      .submit(() -> {
        while (true) {
          try {
            if (updaterList.stream().allMatch(GraphUpdater::isPrimed)) {
              LOG.info("OTP UPDATERS INITIALIZED - OTP is ready for routing!");
              return;
            }
            //noinspection BusyWait
            Thread.sleep(1000);
          } catch (Exception e) {
            LOG.error(e.getMessage(), e);
          }
        }
      });
  }
}
