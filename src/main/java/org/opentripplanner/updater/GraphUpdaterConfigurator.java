package org.opentripplanner.updater;

import org.opentripplanner.ext.examples.updater.ExampleGraphUpdater;
import org.opentripplanner.ext.examples.updater.ExamplePollingGraphUpdater;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.UpdaterConfig;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterConfig;
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
 * Sets up and starts all the graph updaters.
 *
 * Updaters are instantiated based on the updater parameters contained in UpdaterConfig. Updaters
 * are then setup by providing the graph as a parameter. Finally, the updaters are added to the
 * GraphUpdaterManager.
 */
public abstract class GraphUpdaterConfigurator {

    private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterConfigurator.class);

    public static void setupGraph(Graph graph, UpdaterConfig updaterConfig) {

        List<GraphUpdater> updaters = new ArrayList<>();

        updaters.addAll(createUpdatersFromConfig(updaterConfig));

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
     * @return a GraphUpdaterManager containing all the created updaters
     */
    private static List<GraphUpdater> createUpdatersFromConfig(
        UpdaterConfig config
    ) {
        List<GraphUpdater> updaters = new ArrayList<>();

        for (BikeRentalUpdaterConfig configItem : config.getBikeRentalUpdaterConfigList()) {
            updaters.add(new BikeRentalUpdater(configItem));
        }
        for (GtfsRealtimeAlertsUpdaterConfig configItem : config.getGtfsRealtimeAlertsUpdaterConfigList()) {
            updaters.add(new GtfsRealtimeAlertsUpdater(configItem));
        }
        for (PollingStoptimeUpdaterConfig configItem : config.getPollingStoptimeUpdaterConfigList()) {
            updaters.add(new PollingStoptimeUpdater(configItem));
        }
        for (SiriETUpdaterConfig configItem : config.getSiriETUpdaterConfigList()) {
            updaters.add(new SiriETUpdater(configItem));
        }
        for (SiriSXUpdaterConfig configItem : config.getSiriSXUpdaterConfigList()) {
            updaters.add(new SiriSXUpdater(configItem));
        }
        for (SiriVMUpdaterConfig configItem : config.getSiriVMUpdaterConfigList()) {
            updaters.add(new SiriVMUpdater(configItem));
        }
        for (WebsocketGtfsRealtimeUpdaterConfig configItem : config.getWebsocketGtfsRealtimeUpdaterConfigList()) {
            updaters.add(new WebsocketGtfsRealtimeUpdater(configItem));
        }
        for (PollingGraphUpdaterConfig configItem : config.getBikeParkUpdaterConfigList()) {
            updaters.add(new BikeParkUpdater(configItem));
        }
        for (PollingGraphUpdaterConfig configItem : config.getExampleGraphUpdaterConfigList()) {
            updaters.add(new ExampleGraphUpdater(configItem));
        }
        for (PollingGraphUpdaterConfig configItem : config.getExamplePollingGraphUpdaterConfigList()) {
            updaters.add(new ExamplePollingGraphUpdater(configItem));
        }
        for (WFSNotePollingGraphUpdaterConfig configItem : config.getWinkkiPollingGraphUpdaterConfigList()) {
            updaters.add(new WinkkiPollingGraphUpdater(configItem));
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
