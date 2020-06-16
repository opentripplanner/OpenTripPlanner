package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParkingZonesUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingZonesUpdater.class);

    private final ParkingZonesGetter parkingZonesGetter = new ParkingZonesGetter();
    private GraphUpdaterManager graphUpdaterManager;
    private Graph graph;
    private String url;

    @Override
    protected void runPolling() {
        LOG.info("Polling parking zones from API");
        List<GeometryParkingZone> geometryParkingZones = parkingZonesGetter.getParkingZones(url, graph);
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws IllegalStateException {
        this.pollingPeriodSeconds = 60 * 60 * 24;
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
    }

    @Override
    public void teardown() {

    }
}
