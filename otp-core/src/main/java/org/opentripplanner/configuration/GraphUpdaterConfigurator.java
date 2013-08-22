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

package org.opentripplanner.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
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
public class GraphUpdaterConfigurator {

    private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterConfigurator.class);

    private static Map<String, Class<? extends PreferencesConfigurable>> configurables;

    static {
        configurables = new HashMap<String, Class<? extends PreferencesConfigurable>>();
        configurables.put("bike-rental", BikeRentalConfigurator.class);
        configurables.put("stop-time-updater", StopTimeUpdateConfigurator.class);
        configurables.put("real-time-alerts", RealTimeAlertConfigurator.class);
    }

    public void setupGraph(Graph graph, Preferences mainConfig) {
        // Create a periodic updater per graph
        GraphUpdaterManager updaterManager = new GraphUpdaterManager();
        graph.setUpdaterManager(updaterManager);

        // Look for embedded config if it exists
        Properties embeddedGraphPreferences = graph.getEmbeddedPreferences();
        Preferences embeddedConfig = null;
        if (embeddedGraphPreferences != null) {
            embeddedConfig = new PropertiesPreferences(embeddedGraphPreferences);
        }
        LOG.info("Using configurations: " + (mainConfig == null ? "" : "[main]") + " "
                + (embeddedConfig == null ? "" : "[embedded]"));
        applyConfigurationToGraph(graph, Arrays.asList(mainConfig, embeddedConfig));

        // Delete the updater manager if it contains nothing
        if (updaterManager.size() == 0) {
            updaterManager.stop();
            graph.setUpdaterManager(null);
        }
    }

    /**
     * Apply a list of configs to a graph. Please note that the order of the config in the list *is
     * important* as a child node already seen will not be overriden.
     */
    private void applyConfigurationToGraph(Graph graph, List<Preferences> configs) {
        try {
            Set<String> configurableNames = new HashSet<String>();
            for (Preferences config : configs) {
                if (config == null)
                    continue;
                for (String configurableName : config.childrenNames()) {
                    if (configurableNames.contains(configurableName))
                        continue; // Already processed
                    configurableNames.add(configurableName);
                    Preferences prefs = config.node(configurableName);
                    String configurableType = prefs.get("type", null);
                    Class<? extends PreferencesConfigurable> clazz = configurables
                            .get(configurableType);
                    if (clazz != null) {
                        try {
                            LOG.info("Configuring '{}' of type '{}' ({})", configurableName,
                                    configurableType, clazz.getName());
                            PreferencesConfigurable configurable = clazz.newInstance();
                            configurable.configure(graph, prefs);
                        } catch (Exception e) {
                            LOG.error("Can't configure: " + configurableName, e);
                            // Continue on next configurable
                        }
                    }
                }
            }
        } catch (BackingStoreException e) {
            LOG.error("Can't read configuration", e); // Should not happen
        }
    }

    public void shutdownGraph(Graph graph) {
        GraphUpdaterManager updaterManager = graph.getUpdaterManager();
        if (updaterManager != null) {
            LOG.info("Stopping periodic updater with " + updaterManager.size() + " updaters.");
            updaterManager.stop();
        }
    }
}
