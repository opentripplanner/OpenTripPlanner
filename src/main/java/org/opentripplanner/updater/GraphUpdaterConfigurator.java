package org.opentripplanner.updater;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdater;
import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.ext.siri.updater.SiriVMUpdaterParameters;
import org.opentripplanner.ext.vehiclerentalservicedirectory.VehicleRentalServiceDirectoryFetcher;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdater;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdaterParameters;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdaterParameters;
import org.opentripplanner.updater.street_notes.WinkkiPollingGraphUpdater;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingDataSourceFactory;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdater;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdater;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up and starts all the graph updaters.
 *
 * Updaters are instantiated based on the updater parameters contained in UpdaterConfig. Updaters
 * are then setup by providing the graph as a parameter. Finally, the updaters are added to the
 * GraphUpdaterManager.
 */
public abstract class GraphUpdaterConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(GraphUpdaterConfigurator.class);

    public static void setupGraph(
        Graph graph,
        UpdatersParameters updatersParameters
    ) {
        List<GraphUpdater> updaters = new ArrayList<>();

        updaters.addAll(
            createUpdatersFromConfig(updatersParameters)
        );
        updaters.addAll(
            // Setup updaters using the VehicleRentalServiceDirectoryFetcher(Sandbox)
            fetchVehicleRentalServicesFromOnlineDirectory(
                updatersParameters.getVehicleRentalServiceDirectoryFetcherParameters()
            )
        );

        setupUpdaters(graph, updaters);
        GraphUpdaterManager updaterManager = new GraphUpdaterManager(graph, updaters);
        updaterManager.startUpdaters();

        // Stop the updater manager if it contains nothing
        if (updaterManager.numberOfUpdaters() == 0) {
            updaterManager.stop();
        }
        // Otherwise add it to the graph
        else {
            graph.updaterManager = updaterManager;
        }
    }

    public static void shutdownGraph(Graph graph) {
        GraphUpdaterManager updaterManager = graph.updaterManager;
        if (updaterManager != null) {
            LOG.info("Stopping updater manager with " + updaterManager.numberOfUpdaters() + " updaters.");
            updaterManager.stop();
        }
    }

    public static void setupUpdaters(Graph graph, List<GraphUpdater> updaters) {
        for (GraphUpdater updater : updaters) {
            try {
                updater.setup(graph);
            } catch (Exception e) {
                LOG.warn("Failed to setup updater {}", updater.getConfigRef());
            }
        }
    }


    /* private methods */

    /**
     * Use the online UpdaterDirectoryService to fetch VehicleRental updaters.
     */
    private static List<GraphUpdater> fetchVehicleRentalServicesFromOnlineDirectory(
        VehicleRentalServiceDirectoryFetcherParameters parameters
    ) {
        if (parameters == null) { return List.of(); }
        return VehicleRentalServiceDirectoryFetcher.createUpdatersFromEndpoint(parameters);
    }

    /**
     * @return a list of GraphUpdaters created from the configuration
     */
    private static List<GraphUpdater> createUpdatersFromConfig(
        UpdatersParameters config
    ) {
        List<GraphUpdater> updaters = new ArrayList<>();

        for (VehicleRentalUpdaterParameters configItem : config.getVehicleRentalParameters()) {
            var source = VehicleRentalDataSourceFactory.create(configItem.sourceParameters());
            updaters.add(new VehicleRentalUpdater(configItem, source));
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
        for (SiriETGooglePubsubUpdaterParameters configItem : config.getSiriETGooglePubsubUpdaterParameters()) {
            updaters.add(new SiriETGooglePubsubUpdater(configItem));
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
        for (MqttGtfsRealtimeUpdaterParameters configItem : config.getMqttGtfsRealtimeUpdaterParameters()) {
            updaters.add(new MqttGtfsRealtimeUpdater(configItem));
        }
        for (VehicleParkingUpdaterParameters configItem : config.getVehicleParkingUpdaterParameters()) {
            var source = VehicleParkingDataSourceFactory.create(configItem);
            updaters.add(new VehicleParkingUpdater(configItem, source));
        }
        for (WFSNotePollingGraphUpdaterParameters configItem : config.getWinkkiPollingGraphUpdaterParameters()) {
            updaters.add(new WinkkiPollingGraphUpdater(configItem));
        }

        return updaters;
    }
}
