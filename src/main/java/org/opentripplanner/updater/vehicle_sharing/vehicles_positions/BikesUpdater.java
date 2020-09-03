package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.hasura_client.BikeStationsGetter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//TODO one huge todo for miron
public class BikesUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(SharedVehiclesUpdater.class);

    private final BikeStationsGetter bikeStationsGetter = new BikeStationsGetter();
    private TemporaryStreetSplitter temporaryStreetSplitter;
    private GraphUpdaterManager graphUpdaterManager;
    private Graph graph;
    private String url;

    @Override
    protected void runPolling() {
        LOG.info("Polling Bike Stations from API");
        List<BikeRentalStation> bikeRentalStations = bikeStationsGetter.getFromHasura(graph, url);
        LOG.info("Got {} bike stations possible to place on a map", bikeRentalStations.size());
        graphUpdaterManager.execute(new BikeStationsGraphWriterRunnable(temporaryStreetSplitter, bikeRentalStations));
    }


    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws IllegalStateException {
        this.pollingPeriodSeconds = 60;
        this.url = System.getProperty("sharedVehiclesApi");
        if (this.url == null) {
            throw new IllegalStateException("Please provide program parameter `--sharedVehiclesApi <URL>`");
        }
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        configurePolling(graph, config);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.graphUpdaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) throws Exception {
        this.graph = graph;
        this.temporaryStreetSplitter = TemporaryStreetSplitter.createNewDefaultInstance(graph, null, null);
    }

    @Override
    public void teardown() {
    }
}
