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

package org.opentripplanner.api.servlet;

import java.util.LinkedList;
import java.util.List;

import org.opentripplanner.updater.GraphUpdaterRunnable;
import org.opentripplanner.updater.PeriodicTimerGraphUpdater;

/**
 * TODO Remove. This class is kept as is for now only for spring-backward-compatiliby purposes. This
 * class has been replaced by the PeriodicTimerGraphUpdater, with one instance per graph (this class
 * is unique for all graphs and need it's updater to take care of graph eviction).
 * 
 * @see PeriodicTimerGraphUpdater
 */
@Deprecated
public class PeriodicGraphUpdater {
    private UpdateTask updater;

    private long updateFrequency = 1000 * 60 * 5; // five minutes

    private Thread thread;

    private List<GraphUpdaterRunnable> updaters = new LinkedList<GraphUpdaterRunnable>();

    public void start() {
        updater = new UpdateTask();
        thread = new Thread(updater);
        // is there any reason this can't be a daemon thread? seems like a good fit.
        thread.setDaemon(false);
        // useful to name threads for debugging / profiling
        thread.setName("Graph Updater Thread");
        // we assume updaters are set here
        for (GraphUpdaterRunnable updater : updaters) {
            updater.setup();
        }
        thread.start();
    }

    public void stop() {
        updater.finish();
        thread.interrupt();
    }

    public void setUpdateFrequency(long updateFrequency) {
        this.updateFrequency = updateFrequency;
    }

    public long getUpdateFrequency() {
        return updateFrequency;
    }

    public void setUpdaters(List<GraphUpdaterRunnable> updaters) {
        if (updaters != null)
            throw new IllegalArgumentException("Can't set updaters twice.");
        this.updaters = updaters;
    }

    class UpdateTask implements Runnable {
        boolean finished = false;

        public void finish() {
            this.finished = true;
            Thread.currentThread().interrupt();
        }

        @Override
        public void run() {
            long lastUpdate = 0;
            while (!finished) {
                long now = System.currentTimeMillis();
                while (now - lastUpdate < getUpdateFrequency()) {
                    try {
                        Thread.sleep(getUpdateFrequency() - (now - lastUpdate));
                    } catch (InterruptedException e) {
                        if (finished) {
                            break;
                        }
                    }

                    now = System.currentTimeMillis();
                }
                for (GraphUpdaterRunnable updater : updaters) {
                    System.out.println("running updater " + updater);
                    updater.run();
                }

                lastUpdate = now;

                Thread.yield();
            }
            for (GraphUpdaterRunnable updater : updaters) {
                updater.teardown();
            }
        }

    }

}
