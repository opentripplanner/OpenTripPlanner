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

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph updater running a list of Runnable periodically. This class is attached to the graph as a
 * service:
 * 
 * <pre>
 *         PeriodicTimerGraphUpdater periodicGraphUpdater = graph
 *             .getService(PeriodicTimerGraphUpdater.class);
 * </pre>
 * 
 * 
 * Each Runnable can have it's own frequency. We rely on standard Java Timer for implementation.
 * 
 */
public class PeriodicTimerGraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(PeriodicTimerGraphUpdater.class);

    private Timer timer = new Timer(true);

    private int size = 0;

    public PeriodicTimerGraphUpdater() {
    }

    public void stop() {
        timer.cancel();
    }

    public void addUpdater(final Runnable updater, long frequencyMs) {
        timer.scheduleAtFixedRate(new TimerTask() {
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
        }, 0, frequencyMs);
        size++;
    }

    public int size() {
        return size;
    }

}
