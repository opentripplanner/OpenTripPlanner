package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.emptyList;

public class ParkingZonesUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingZonesUpdater.class);

    private final ParkingZonesGetter parkingZonesGetter = new ParkingZonesGetter();
    private GraphUpdaterManager graphUpdaterManager;
    private Graph graph;
    private String url;

    private RentVehicleAnywhereEdge getRentVehicleAnywhereEdge(Vertex v) {
        return v.getOutgoing().stream()
                .filter(RentVehicleAnywhereEdge.class::isInstance)
                .map(RentVehicleAnywhereEdge.class::cast)
                .findAny()
                .orElse(null);
    }

    private List<Pair<RentVehicleAnywhereEdge, List<SingleParkingZone>>> getNewParkingZones(ParkingZones parkingZones) {
        graph.getVertices().stream()
                .map(this::getRentVehicleAnywhereEdge)
                .filter(Objects::nonNull);
        return emptyList(); // TODO AdamWiktor
    }

    private List<SingleParkingZone> getNewParkingZonesEnabled(ParkingZones parkingZones) {
        return emptyList(); // TODO AdamWiktor
    }

    @Override
    protected void runPolling() {
        LOG.info("Polling parking zones from API");
        ParkingZones parkingZones = parkingZonesGetter.getParkingZones(graph, url);
        List<SingleParkingZone> parkingZonesEnabled = getNewParkingZonesEnabled(parkingZones);
        List<Pair<RentVehicleAnywhereEdge, List<SingleParkingZone>>> parkingZonesPerVertex =
                getNewParkingZones(parkingZones);

        ParkingZonesGraphWriterRunnable graphWriterRunnable =
                new ParkingZonesGraphWriterRunnable(parkingZonesPerVertex, parkingZonesEnabled);
        LOG.info("Got new parking zones");
        graphUpdaterManager.execute(graphWriterRunnable);
        LOG.info("Finished updating parking zones");
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
