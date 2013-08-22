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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is attached to the graph:
 * 
 * <pre>
 * GraphUpdaterManager updaterManager = graph.getUpdaterManager();
 * </pre>
 * 
 * Each updater will run in its own thread. When changes to the graph have to be made, this should 
 * be done via the scheduler to prevent race conditions between multiple updaters. 
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
    
    private ExecutorService updaterPool = Executors.newCachedThreadPool(); 
    
    List<GraphUpdaterRunnable> updaterList = new ArrayList<GraphUpdaterRunnable>();

    public GraphUpdaterManager() {
    }

    public void stop() {
        // Shutdown updaters
        for (GraphUpdaterRunnable updater : updaterList) {
            updater.teardown();
        }

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

    public void addUpdater(final GraphUpdaterRunnable updater, long frequencyMs) {
        updater.setup();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    updater.run();
                } catch (Exception e) {
                    LOG.error("Error while running updater " + updater.getClass().getName(), e);
                    // TODO Should we cancel the task? Or after n consecutive failures?
                    // cancel();
                }
            }
        }, 0, frequencyMs, TimeUnit.MILLISECONDS);
        updaterList.add(updater);
    }
    
//    public void 

    public int size() {
        return updaterList.size();
    }

}
