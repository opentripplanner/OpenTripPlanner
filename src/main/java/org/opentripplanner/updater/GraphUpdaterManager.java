package org.opentripplanner.updater;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
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
public class GraphUpdaterManager {

    private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterManager.class);
    
    /**
     * Text used for naming threads when the graph lacks a routerId.
     */
    private static String DEFAULT_ROUTER_ID = "(default)";
    
    /**
     * Thread factory used to create new threads, giving them more human-readable names including the routerId.
     */
    private ThreadFactory threadFactory;

    /**
     * OTP's multi-version concurrency control model for graph updating allows simultaneous reads,
     * but never simultaneous writes. We ensure this policy is respected by having a single writer
     * thread, which sequentially executes all graph updater tasks. Each task is a runnable that is
     * scheduled with the ExecutorService to run at regular intervals.
     * FIXME in reality we're not using scheduleAtFixedInterval. We're scheduling for immediate execution from separate threads that sleep in a loop.
     */
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * A pool of threads on which the updaters will run.
     * This creates a pool that will auto-scale up to any size (maximum pool size is MAX_INT).
     * FIXME The polling updaters occupy an entire thread, sleeping in between polling operations.
     */
    private ExecutorService updaterPool = Executors.newCachedThreadPool();

    /**
     * Keep track of all updaters so we can cleanly free resources associated with them at shutdown.
     */
    private List<GraphUpdater> updaterList = new ArrayList<>();

    /**
     * The Graph that will be updated.
     */
    private Graph graph;

    /**
     * Constructor.
     * @param graph is the Graph that will be updated.
     */
    public GraphUpdaterManager(Graph graph) {
        this.graph = graph;
        
        String routerId = graph.routerId;
        if(routerId == null || routerId.isEmpty())
            routerId = DEFAULT_ROUTER_ID;
        
        threadFactory = new ThreadFactoryBuilder().setNameFormat("GraphUpdater-" + routerId + "-%d").build();
        scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        updaterPool = Executors.newCachedThreadPool(threadFactory);
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
     * Adds an updater to the manager and runs it immediately in its own thread.
     * 
     * @param updater is the updater to add and run
     */
    public void addUpdater(final GraphUpdater updater) {
        updaterList.add(updater);
    }

    /**
     * This is the method to use to modify the graph from the updaters. The runnables will be
     * scheduled after each other, guaranteeing that only one of these runnables will be active at
     * any time.
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
        if (id >= updaterList.size()) return null;
        return updaterList.get(id);
    }
}
