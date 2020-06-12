package org.opentripplanner.updater;

import org.opentripplanner.ext.examples.updater.ExampleGraphUpdater;
import org.opentripplanner.ext.examples.updater.ExamplePollingGraphUpdater;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.ext.updaterendpointfetcher.GBFSUpdaterEndpointFetcher;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.UpdaterConfig;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.bike_park.BikeParkUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.street_notes.WinkkiPollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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

    public static void setupGraph(
        Graph graph,
        UpdaterConfig updaterConfig,
        URI bikeShareEndpoint
    ) {

        List<GraphUpdater> updaters = new ArrayList<>(createUpdatersFromConfig(updaterConfig));

        if (bikeShareEndpoint != null) {
            updaters.addAll(GBFSUpdaterEndpointFetcher.createUpdatersFromEndpoint(bikeShareEndpoint));
        }

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
     * @return a list of GraphUpdaters created from the configuration
     */
    private static List<GraphUpdater> createUpdatersFromConfig(
        UpdaterConfig config
    ) {
        List<GraphUpdater> updaters = new ArrayList<>();

        for (BikeRentalUpdaterParameters configItem : config.getBikeRentalParameters()) {
            updaters.add(new BikeRentalUpdater(configItem));
        }
        for (GtfsRealtimeAlertsUpdaterParameters configItem : config.getGtfsRealtimeAlertsUpdaterParameters()) {
            updaters.add(new GtfsRealtimeAlertsUpdater(configItem));
        }
        for (PollingStoptimeUpdaterParameters configItem : config.getPollingStoptimeUpdaterParameters()) {
            updaters.add(new PollingStoptimeUpdater(configItem));
        }
        for (SiriETUpdaterParameters configItem : config.getSiriETUpdaterParameters()) {
            updaters.add(new SiriETUpdater(configItem));
        }
        for (SiriSXUpdaterParameters configItem : config.getSiriSXUpdaterParameters()) {
            updaters.add(new SiriSXUpdater(configItem));
        }
        for (SiriVMUpdaterParameters configItem : config.getSiriVMUpdaterParameters()) {
            updaters.add(new SiriVMUpdater(configItem));
        }
        for (WebsocketGtfsRealtimeUpdaterParameters configItem : config.getWebsocketGtfsRealtimeUpdaterParameters()) {
            updaters.add(new WebsocketGtfsRealtimeUpdater(configItem));
        }
        for (PollingGraphUpdaterParameters configItem : config.getBikeParkUpdaterParameters()) {
            updaters.add(new BikeParkUpdater(configItem));
        }
        for (PollingGraphUpdaterParameters configItem : config.getExampleGraphUpdaterParameters()) {
            updaters.add(new ExampleGraphUpdater(configItem));
        }
        for (PollingGraphUpdaterParameters configItem : config.getExamplePollingGraphUpdaterParameters()) {
            updaters.add(new ExamplePollingGraphUpdater(configItem));
        }
        for (WFSNotePollingGraphUpdaterParameters configItem : config.getWinkkiPollingGraphUpdaterParameters()) {
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
