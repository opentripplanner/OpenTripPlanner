package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SharedVehiclesUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(SharedVehiclesUpdater.class);

    private final VehiclePositionsGetter vehiclePositionsGetter = new VehiclePositionsGetter();
    private TemporaryStreetSplitter temporaryStreetSplitter;
    private GraphUpdaterManager graphUpdaterManager;
    private Graph graph;
    private String url;

    @Override
    protected void runPolling() {
        LOG.info("Polling vehicles from API");
        List<VehicleDescription> vehicles = vehiclePositionsGetter.getVehicleDescriptions(graph, url);
        LOG.info("Got {} vehicles possible to place on a map", vehicles.size());
        graphUpdaterManager.execute(new VehicleSharingGraphWriterRunnable(temporaryStreetSplitter, vehicles));
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
