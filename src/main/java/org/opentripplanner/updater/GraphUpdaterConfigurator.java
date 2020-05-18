package org.opentripplanner.updater;

import org.opentripplanner.ext.examples.updater.ExampleGraphUpdater;
import org.opentripplanner.ext.examples.updater.ExamplePollingGraphUpdater;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.updater_config.UpdaterConfig;
import org.opentripplanner.standalone.config.updater_config.UpdaterConfigItem;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.bike_park.BikeParkUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.street_notes.WinkkiPollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public static void setupGraph(Graph graph, UpdaterConfig updaterConfig) {

        List<GraphUpdater> updaters = new ArrayList<>();

        updaters.addAll(createUpdatersFromConfig(graph, updaterConfig));

        setupUpdaters(graph, updaters);
        GraphUpdaterManager updaterManager = new GraphUpdaterManager(graph, updaters);
        updaterManager.startUpdaters();

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
    private static List<GraphUpdater> createUpdatersFromConfig(Graph graph, UpdaterConfig config) {

        List<GraphUpdater> updaters = new ArrayList<>();

        for (UpdaterConfigItem configItem : config.getItems()) {

            // For each sub-node, determine which kind of updater is being created.
            String type = configItem.getType();
            GraphUpdater updater = null;

            try {
                if (type != null) {
                    switch (type) {
                        case "bike-rental":
                            updater = new BikeRentalUpdater();
                            ((BikeRentalUpdater) updater).configure(graph, configItem);
                            break;
                        case "bike-park":
                            updater = new BikeParkUpdater();
                            ((BikeParkUpdater) updater).configure(graph, configItem);
                            break;
                        case "stop-time-updater":
                            updater = new PollingStoptimeUpdater();
                            ((PollingStoptimeUpdater) updater).configure(graph, configItem);
                            break;
                        case "websocket-gtfs-rt-updater":
                            updater = new WebsocketGtfsRealtimeUpdater();
                            ((WebsocketGtfsRealtimeUpdater) updater).configure(graph, configItem);
                            break;
                        case "real-time-alerts":
                            updater = new GtfsRealtimeAlertsUpdater();
                            ((GtfsRealtimeAlertsUpdater) updater).configure(graph, configItem);
                            break;
                        case "example-updater":
                            updater = new ExampleGraphUpdater();
                            ((ExampleGraphUpdater) updater).configure(graph, configItem);
                            break;
                        case "example-polling-updater":
                            updater = new ExamplePollingGraphUpdater();
                            ((ExamplePollingGraphUpdater) updater).configure(graph, configItem);
                            break;
                        case "winkki-polling-updater":
                            updater = new WinkkiPollingGraphUpdater();
                            ((WinkkiPollingGraphUpdater) updater).configure(graph, configItem);
                            break;
                        case "siri-et-updater":
                            updater = new SiriETUpdater();
                            ((SiriETUpdater) updater).configure(graph, configItem);
                            break;
                        case "siri-vm-updater":
                            updater = new SiriVMUpdater();
                            ((SiriVMUpdater) updater).configure(graph, configItem);
                            break;
                        case "siri-sx-updater":
                            updater = new SiriSXUpdater();
                            ((SiriSXUpdater) updater).configure(graph, configItem);
                            break;
                    }
                }
                if (updater != null) {
                    updaters.add(updater);
                }
            }
            catch (Exception e) {
                LOG.error("Failed to configure graph updater:" + configItem.getType(), e);
            }
        }

        return updaters;
    }

    public static void shutdownGraph(Graph graph) {
        GraphUpdaterManager updaterManager = graph.updaterManager;
        if (updaterManager != null) {
            LOG.info("Stopping updater manager with " + updaterManager.size() + " updaters.");
            updaterManager.stop();
        }
    }

    public static void setupUpdaters(Graph graph, List<GraphUpdater> updaters) {
        for (GraphUpdater updater : updaters) {
            try {
                updater.setup(graph);
            } catch (Exception e) {
                LOG.warn("Failed to setup updater {}", updater.getName());
            }
        }
    }
}
