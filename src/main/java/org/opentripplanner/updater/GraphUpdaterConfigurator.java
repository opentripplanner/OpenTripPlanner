package org.opentripplanner.updater;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.annotation.ComponentAnnotationConfigurator;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upon loading a Graph, configure/decorate it using a JSON tree from Jackson. This mainly involves starting
 * graph updater processes (GTFS-RT, bike rental, etc.), hence the class name.
 *
 * When a Graph is loaded, one should call setupGraph() with the JSON tree containing configuration for the Graph.
 * That method creates "graph updaters" according to the given JSON, which should contain an array or object field
 * called "updaters". Each child element represents one updater.
 *
 * When a graph is unloaded, one must ensure the shutdownGraph() method is called to clean up all resources that may
 * have been used.
 *
 * If an embedded configuration is present in the graph, we also try to use it. In case of conflicts
 * between two child nodes in both configs (two childs node with the same name) the dynamic (ie
 * provided) configuration takes complete precedence over the embedded one: childrens properties are
 * *not* merged.
 */
public abstract class GraphUpdaterConfigurator {

    private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterConfigurator.class);

    public static void setupGraph(Graph graph, JsonNode mainConfig) {

        // Look for embedded config if it exists
        // TODO figure out how & when we will use embedded config in absence of main config.
        JsonNode embeddedConfig = null; // graph.routerConfig;
        LOG.info("Using configurations: " + (mainConfig == null ? "" : "[main]") + " "
                + (embeddedConfig == null ? "" : "[embedded]"));

        // Create a updater manager for this graph, and create updaters according to the JSON configuration.
        GraphUpdaterManager updaterManager = createManagerFromConfig(graph, mainConfig);

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
     * @param graph the graph that will be modified by these updaters
     * @return a GraphUpdaterManager containing all the created updaters
     */
    private static GraphUpdaterManager createManagerFromConfig(Graph graph, JsonNode config) {

        GraphUpdaterManager updaterManager = new GraphUpdaterManager(graph);
        for (JsonNode configItem : config.path("updaters")) {
            // For each sub-node, determine which kind of updater is being created.
            String type = configItem.path("type").asText();
            if (type != null) {
                try {
                    GraphUpdater updater = ComponentAnnotationConfigurator.getInstance()
                        .getComponentInstance(type, ServiceType.GraphUpdater);
                    // Inform the GraphUpdater of its parent Manager so the updater can enqueue write operations.
                    // Perhaps this should be done in "addUpdater" below, to ensure the link is reciprocal.
                    updater.setGraphUpdaterManager(updaterManager);
                    // All GraphUpdaters are JsonConfigurable - send them their config information.
                    updater.configure(graph, configItem);
                    // Perform any initial setup in a single-threaded manner to avoid concurrent reads/writes.
                    updater.setup(graph);
                    // Add graph updater to manager.
                    updaterManager.addUpdater(updater);
                    LOG.info("Configured GraphUpdater: {}", updater);
                } catch (Exception e) {
                    LOG.error("Failed to configure graph updater:" + configItem.asText(), e);
                }
            }
        }
        // Now that all the updaters are configured, kick them all off in their own threads.
        updaterManager.startUpdaters();
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
