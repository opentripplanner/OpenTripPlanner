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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.bike_park.BikeParkUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.updater.example.ExampleGraphUpdater;
import org.opentripplanner.updater.example.ExamplePollingGraphUpdater;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.street_notes.WinkkiPollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure/decorate a graph upon loading through Preferences (Preference is the new Java API
 * replacing "Properties"). Usually preferences are loaded from a .properties files, but could also
 * come from the graph itself or any other sources.
 * 
 * When a graph is loaded, client should call setupGraph() with the Preferences setup.
 * 
 * When a graph is unloaded, one must ensure the shutdownGraph() method is called to cleanup all
 * resources that could have been created.
 * 
 * This class then creates "graph updaters" (usually real-time connector, etc...) depending on the
 * given configuration, and configure them using the corresponding children Preferences node.
 * 
 * If an embedded configuration is present in the graph, we also try to use it. In case of conflicts
 * between two child nodes in both configs (two childs node with the same name) the dynamic (ie
 * provided) configuration takes complete precedence over the embedded one: childrens properties are
 * *not* merged.
 * 
 */
public abstract class GraphUpdaterConfigurator {

    private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterConfigurator.class);

    public static void setupGraph(Graph graph, JsonNode mainConfig) {
        // Create a updater manager for this graph
        GraphUpdaterManager updaterManager = new GraphUpdaterManager(graph);

        // Look for embedded config if it exists
        // TODO figure out how we will use embedded config in absence of main config.
        JsonNode embeddedConfig = graph.embeddedPreferences;
        LOG.info("Using configurations: " + (mainConfig == null ? "" : "[main]") + " "
                + (embeddedConfig == null ? "" : "[embedded]"));
        
        // Apply configuration
        // FIXME why are we returning the same updatermanager object that has been modified ? this method could just create it.
        updaterManager = applyConfigurationToGraph(graph, updaterManager, mainConfig);

        // Stop the updater manager if it contains nothing
        if (updaterManager.size() == 0) {
            updaterManager.stop();
        }
        // Otherwise add it to the graph
        else {
            graph.updaterManager = updaterManager;
        }
    }

    /**
     * @param graph
     * @param updaterManager is the graph updater manager to which all updaters should be added
     * @return reference to the same updaterManager as was given as input
     */
    private static GraphUpdaterManager applyConfigurationToGraph(Graph graph, GraphUpdaterManager updaterManager, JsonNode config) {

        for (JsonNode configItem : config.path("updaters")) {

            // For each sub-node, determine which kind of updater is being created.
            String type = configItem.path("type").asText();
            GraphUpdater updater = null;
            if (type != null) {
                if (type.equals("bike-rental")) {
                    updater = new BikeRentalUpdater();
                }
                else if (type.equals("bike-park")) {
                    updater = new BikeParkUpdater();
                }
                else if (type.equals("stop-time-updater")) {
                    updater = new PollingStoptimeUpdater();
                }
                else if (type.equals("websocket-gtfs-rt-updater")) {
                    updater = new WebsocketGtfsRealtimeUpdater();
                }
                else if (type.equals("real-time-alerts")) {
                    updater = new GtfsRealtimeAlertsUpdater();
                }
                else if (type.equals("example-updater")) {
                    updater = new ExampleGraphUpdater();
                }
                else if (type.equals("example-polling-updater")) {
                    updater = new ExamplePollingGraphUpdater();
                }
                else if (type.equals("winkki-polling-updater")) {
                    updater = new WinkkiPollingGraphUpdater();
                }
            }

            // Configure and activate the new updater.
            try {
                // Check whether no updater type was found
                if (updater == null) {
                    LOG.error("Unknown updater type: " + type);
                } else {
                    // Add manager as parent
                    updater.setGraphUpdaterManager(updaterManager);
                    // Configure updater if found and necessary
                    if (updater instanceof JsonConfigurable) {
                        ((JsonConfigurable) updater).configure(graph, configItem);
                    }
                    // Add graph updater to manager
                    updaterManager.addUpdater(updater);
                    LOG.info ("Configured GraphUpdater: {}", updater);
                }
            } catch (Exception e) {
                LOG.error("Can't configure: " + configItem.asText(), e);
                // Continue on to the next node
            }
        }
        return updaterManager;
    }

    public static void shutdownGraph(Graph graph) {
        GraphUpdaterManager updaterManager = graph.updaterManager;
        if (updaterManager != null) {
            LOG.info("Stopping updater manager with " + updaterManager.size() + " updaters.");
            updaterManager.stop();
        }
    }
}
