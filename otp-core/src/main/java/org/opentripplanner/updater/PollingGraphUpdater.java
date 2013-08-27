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

import java.util.prefs.Preferences;

import lombok.Getter;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements logic that is shared between all polling updaters.
 * 
 * Usage example ('polling' name is an example and 'polling-updater' should be the type of a
 * concrete class derived from this abstract class):
 * 
 * <pre>
 * polling.type = polling-updater
 * polling.frequencySec = 60
 * </pre>
 * 
 * @see GraphUpdater
 */
public abstract class PollingGraphUpdater implements GraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(PollingGraphUpdater.class);

    /**
     * Mirrors GraphUpdater.run method. Only difference is that runPolling will be run multiple
     * times with pauses in between. The length of the pause is defined in the preference
     * frequencySec.
     */
    abstract protected void runPolling() throws Exception;

    /**
     * Mirrors GraphUpdater.configure method.
     */
    abstract protected void configurePolling(Graph graph, Preferences preferences) throws Exception;

    /**
     * The number of seconds between two polls
     */
    @Getter
    private Integer frequencySec;

    /**
     * The type name in the preferences
     */
    private String type;

    @Override
    final public void run() {
        try {
            // Run "forever"
            while (true) {
                try {
                    // Run concrete class' method
                    runPolling();
                } catch (InterruptedException e) {
                    // Throw further up the stack
                    throw e;
                } catch (Exception e) {
                    LOG.error("Error while running polling updater of type {}", type, e);
                    // TODO Should we cancel the task? Or after n consecutive failures?
                    // cancel();
                }

                // Sleep a given number of seconds
                Thread.sleep(frequencySec * 1000);
            }
        } catch (InterruptedException e) {
            // When updater is interrupted
            LOG.error("Polling updater {} is interrupted, updater stops.", this.getClass()
                    .getName());
        }
    }

    @Override
    final public void configure(Graph graph, Preferences preferences) throws Exception {
        // Configure polling system
        frequencySec = preferences.getInt("frequencySec", 60);
        type = preferences.get("type", "");
        // Configure concrete class
        configurePolling(graph, preferences);
    }
}
