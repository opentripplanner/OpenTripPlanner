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

package org.opentripplanner.decoration;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.PeriodicTimerGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorate a graph upon loading through Preferences (Preference is the new Java API replacing
 * "Properties"). Usually preferences are loaded from a .properties files, but could also come from
 * the graph itself or any other sources.
 * 
 * When a graph is loaded, client should call setupGraph() with the Preferences setup.
 * 
 * When a graph is unloaded, one must ensure the shutdownGraph() method is called to cleanup all
 * resources that could have been created.
 * 
 * This class then create "beans" (usually real-time connector, etc...) depending on the given
 * configuration, and configure them using the corresponding children Preferences node.
 * 
 */
public class GraphDecorator {

    private static Logger LOG = LoggerFactory.getLogger(GraphDecorator.class);

    private static Map<String, Class<? extends Configurable>> configurables;

    static {
        configurables = new HashMap<String, Class<? extends Configurable>>();
        // TODO Add new configurables: real-time
        configurables.put("bike-rental", BikeRentalDecorator.class);
    }

    public void setupGraph(Graph graph, Preferences config) {
        try {
            // Create a periodic updater per graph
            PeriodicTimerGraphUpdater periodicUpdater = graph.getService(
                    PeriodicTimerGraphUpdater.class, true);

            for (String beanName : config.childrenNames()) {
                Preferences beanConfig = config.node(beanName);
                String beanType = beanConfig.get("type", null);
                Class<? extends Configurable> clazz = configurables.get(beanType);
                if (clazz != null) {
                    try {
                        LOG.info("Configuring bean {} of type {} ({})", beanName, beanType,
                                clazz.getName());
                        Configurable bean = clazz.newInstance();
                        bean.configure(graph, beanConfig);
                    } catch (Exception e) {
                        LOG.error("Can't configure bean: " + beanName, e);
                        // Continue on next bean
                    }
                }
            }
            // Delete the periodic updater if it contains nothing
            if (periodicUpdater.size() == 0) {
                graph.putService(PeriodicTimerGraphUpdater.class, null);
            }

        } catch (BackingStoreException e) {
            LOG.error("Can't read configuration", e); // Should not happen
        }
    }

    public void shutdownGraph(Graph graph) {
        ShutdownGraphService shutdownGraphService = graph.getService(ShutdownGraphService.class);
        if (shutdownGraphService != null) {
            shutdownGraphService.shutdown(graph);
        }
        PeriodicTimerGraphUpdater periodicUpdater = graph
                .getService(PeriodicTimerGraphUpdater.class);
        if (periodicUpdater != null) {
            LOG.info("Stopping periodic updater with " + periodicUpdater.size() + " updaters.");
            periodicUpdater.stop();
        }
    }
}
