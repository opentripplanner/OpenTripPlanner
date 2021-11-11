package org.opentripplanner.updater;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is attached to the graph:
 * 
 * <pre>
 * GraphUpdaterManager updaterManager = graph.getUpdaterManager();
 * </pre>
 * 
 * Each updater will run in its own thread. When changes to the graph have to be made by these
 * updaters, this should be done via the execute method of this manager to prevent race conditions
 * between graph write operations.
 * 
 */
public class GraphUpdaterManager implements WriteToGraphCallback {

    private static final Logger LOG = LoggerFactory.getLogger(GraphUpdaterManager.class);

    /**
     * OTP's multi-version concurrency control model for graph updating allows simultaneous reads,
     * but never simultaneous writes. We ensure this policy is respected by having a single writer
     * thread, which sequentially executes all graph updater tasks. Each task is a runnable that is
     * scheduled with the ExecutorService to run at regular intervals.
     * FIXME: In reality we're not using scheduleAtFixedInterval.
     *        We're scheduling for immediate execution from separate threads that sleep in a loop.
     *        We should perhaps switch to having polling GraphUpdaters call scheduleAtFixedInterval.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * A pool of threads on which the updaters will run.
     * This creates a pool that will auto-scale up to any size (maximum pool size is MAX_INT).
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

    /**
     * Constructor.
     * @param graph is the Graph that will be updated.
     */
    public GraphUpdaterManager(Graph graph, List<GraphUpdater> updaters) {
        this.graph = graph;
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
     * This should be called only once at startup to kick off every updater in its own thread, and only after all
     * the updaters have had their setup methods called.
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

    /**
     * This is the method to use to modify the graph from the updaters. The runnables will be
     * scheduled after each other, guaranteeing that only one of these runnables will be active at
     * any time. If a particular GraphUpdater calls this method on more than one GraphWriterRunnable, they should be
     * executed in the same order that GraphUpdater made the calls.
     *
     * @param runnable is a graph writer runnable
     */
    public void execute(GraphWriterRunnable runnable) {
        scheduler.submit(() -> {
            try {
                runnable.run(graph);
            } catch (Exception e) {
                LOG.error("Error while running graph writer {}:", runnable.getClass().getName(), e);
            }
        });
    }

    public int size() {
        return updaterList.size();
    }

    /**
     * Just an example of fetching status information from the graph updater manager to expose it in a web service.
     * More useful stuff should be added later.
     */
    public Map<Integer, String> getUpdaterDescriptions () {
        Map<Integer, String> ret = Maps.newTreeMap();
        int i = 0;
        for (GraphUpdater updater : updaterList) {
            ret.put(i++, updater.toString());
        }
        return ret;
    }

    /**
     * Just an example of fetching status information from the graph updater manager to expose it in a web service.
     * More useful stuff should be added later.
     */
    public GraphUpdater getUpdater (int id) {
        if (id >= updaterList.size()) { return null; }
        return updaterList.get(id);
    }

    public List<GraphUpdater> getUpdaterList() {
        return updaterList;
    }

    public Collection<String> waitingUpdaters() {
        Collection<String> waitingUpdaters = new ArrayList<>();
        for (GraphUpdater updater : graph.updaterManager.getUpdaterList()) {
            if (!(updater).isPrimed()) {
                waitingUpdaters.add(updater.getConfigRef());
            }
        }
        return waitingUpdaters;
    }

    public ExecutorService getUpdaterPool() {
        return updaterPool;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
