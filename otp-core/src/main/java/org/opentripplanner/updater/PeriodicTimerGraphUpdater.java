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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph updater running a list of GraphUpdaterRunnable periodically. This class is attached to the
 * graph as a service:
 * 
 * <pre>
 * PeriodicTimerGraphUpdater periodicGraphUpdater = graph.getService(PeriodicTimerGraphUpdater.class);
 * </pre>
 * 
 * Each GraphUpdaterRunnable can have it's own frequency. We rely on standard Java
 * ScheduledExecutorService for implementation.
 * 
 */
public class PeriodicTimerGraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(PeriodicTimerGraphUpdater.class);

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    List<GraphUpdaterRunnable> updaters = new ArrayList<GraphUpdaterRunnable>();

    public PeriodicTimerGraphUpdater() {
    }

    public void stop() {
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
        for (GraphUpdaterRunnable updater : updaters) {
            updater.teardown();
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
        updaters.add(updater);
    }

    public int size() {
        return updaters.size();
    }

}
