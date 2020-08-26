package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.hasura_client.BikeStationsGetter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws IllegalStateException {
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
    }

    @Override
    public void setup(Graph graph) throws Exception {
    }

    @Override
    public void teardown() {
    }
}
