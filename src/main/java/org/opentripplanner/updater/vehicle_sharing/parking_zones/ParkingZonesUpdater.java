package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

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

    private boolean isPointInParkingZone(Point point, GeometryParkingZone gpz) {
        return gpz.getGeometriesAllowed().stream().anyMatch(g -> g.contains(point))
                && gpz.getGeometriesDisallowed().stream().noneMatch(g -> g.contains(point));
    }

    private SingleParkingZone getMatchingParkingZoneFromList(GeometryParkingZone geometryParkingZone, List<SingleParkingZone> parkingZonesEnabled) {
        return parkingZonesEnabled.stream()
                .filter(pz -> pz.sameProviderIdAndVehicleType(geometryParkingZone))
                .findFirst()
                .orElse(null);
    }

    private SingleParkingZone findMatchingParkingZone(Point point, GeometryParkingZone geometryParkingZone, List<SingleParkingZone> parkingZonesEnabled) {
        if (isPointInParkingZone(point, geometryParkingZone)) {
            return getMatchingParkingZoneFromList(geometryParkingZone, parkingZonesEnabled);
        } else {
            return null;
        }
    }

    private Point createPoint(RentVehicleAnywhereEdge e) {
        CoordinateXY coord = new CoordinateXY(e.getFromVertex().getLon(), e.getFromVertex().getLat());
        return new Point(new CoordinateArraySequence(new Coordinate[]{coord}), new GeometryFactory());
    }

    private List<SingleParkingZone> getParkingZonesForRentEdge(RentVehicleAnywhereEdge e, List<GeometryParkingZone> geometryParkingZones, List<SingleParkingZone> parkingZonesEnabled) {
        Point point = createPoint(e);
        return geometryParkingZones.stream()
                .map(gpz -> findMatchingParkingZone(point, gpz, parkingZonesEnabled))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Map<RentVehicleAnywhereEdge, List<SingleParkingZone>> getNewParkingZones(List<GeometryParkingZone> geometryParkingZones, List<SingleParkingZone> parkingZonesEnabled) {
        return graph.getVertices().stream()
                .map(this::getRentVehicleAnywhereEdge)
                .filter(Objects::nonNull)
                .collect(toMap(identity(), e -> getParkingZonesForRentEdge(e, geometryParkingZones, parkingZonesEnabled)));
    }

    private List<SingleParkingZone> getNewParkingZonesEnabled(List<GeometryParkingZone> geometryParkingZones) {
        return geometryParkingZones.stream()
                .map(gpz -> new SingleParkingZone(gpz.getProviderId(), gpz.getVehicleType()))
                .distinct()
                .collect(toList());
    }

    @Override
    protected void runPolling() {
        LOG.info("Polling parking zones from API");
        List<GeometryParkingZone> geometryParkingZones = parkingZonesGetter.getParkingZones(url, graph);
        LOG.info("Grouping parking zones");
        List<SingleParkingZone> parkingZonesEnabled = getNewParkingZonesEnabled(geometryParkingZones);
        LOG.info("Calculating parking zones for each vertex");
        Map<RentVehicleAnywhereEdge, List<SingleParkingZone>> parkingZonesPerVertex =
                getNewParkingZones(geometryParkingZones, parkingZonesEnabled);
        ParkingZonesGraphWriterRunnable graphWriterRunnable =
                new ParkingZonesGraphWriterRunnable(parkingZonesPerVertex, parkingZonesEnabled);
        LOG.info("Executing parking zones update");
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
