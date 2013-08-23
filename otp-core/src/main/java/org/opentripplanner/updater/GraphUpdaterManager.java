/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater;

import java.util.ArrayList;
import java.util.List;
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
public class GraphUpdaterManager {

    private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterManager.class);

    /** 
     * OTP's multi-version concurrency control model for graph updating allows simultaneous reads,
     * but never simultaneous writes. We ensure this policy is respected by having a single writer 
     * thread, which sequentially executes all graph updater tasks. Each task is a runnable that is
     * scheduled with the ExecutorService to run at regular intervals.
     */
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Pool with updaters
     */
    private ExecutorService updaterPool = Executors.newCachedThreadPool(); 
    
    /**
     * List with updaters to be able to free resources
     * TODO: is this list necessary?
     */
    List<GraphUpdater> updaterList = new ArrayList<GraphUpdater>();
    
    /**
     * Parent graph of this manager
     */
    Graph graph;

    /**
     * Constructor
     * @param graph is parent graph of manager
     */
    public GraphUpdaterManager(Graph graph) {
        this.graph = graph;
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

    public void addUpdater(final GraphUpdater updater) {
        updaterPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    updater.setup();
                    updater.run();
                } catch (Exception e) {
                    LOG.error("Error while running updater " + updater.getClass().getName(), e);
                }
            }
        });
        updaterList.add(updater);
    }
    
    /**
     * This is the method to use to modify the graph from the updaters. The runnables will be
     * scheduled after each other, guaranteeing that only one of these runnables will be active at
     * any time.
     * 
     * @param runnable is a graph writer runnable
     */
    public void execute(final GraphWriterRunnable runnable) {
        // TODO: check for high water mark?
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run(graph);
                } catch (Exception e) {
                    LOG.error("Error while running graph writer " + runnable.getClass().getName(), e);
                }
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    public int size() {
        return updaterList.size();
    }

}
