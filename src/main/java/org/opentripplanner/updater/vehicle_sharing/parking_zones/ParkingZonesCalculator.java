package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import com.google.common.annotations.VisibleForTesting;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opentripplanner.routing.edgetype.rentedgetype.EdgeWithParkingZones;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo;
import org.opentripplanner.routing.edgetype.rentedgetype.SingleParkingZone;
import org.opentripplanner.routing.graph.Graph;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class ParkingZonesCalculator {

    private final List<GeometryParkingZone> geometryParkingZones;

    @VisibleForTesting
    List<SingleParkingZone> parkingZonesEnabled;
    private final List<SingleParkingZone> additionalParkingZonesEnabled = new LinkedList<>();

    public ParkingZonesCalculator(List<GeometryParkingZone> geometryParkingZones) {
        this.geometryParkingZones = geometryParkingZones;
        this.parkingZonesEnabled = createParkingZonesEnabled();
        this.parkingZonesEnabled.addAll(additionalParkingZonesEnabled);
    }

    private List<SingleParkingZone> createParkingZonesEnabled() {
        return geometryParkingZones.stream()
                .map(gpz -> new SingleParkingZone(gpz.getProviderId(), gpz.getVehicleType()))
                .distinct()
                .collect(toList());
    }

    public ParkingZoneInfo getParkingZonesForEdge(EdgeWithParkingZones edge) {
        Point point = createPoint(edge);
        List<SingleParkingZone> parkingZones = geometryParkingZones.stream()
                .map(gpz -> findMatchingParkingZone(point, gpz))
                .filter(Objects::nonNull)
                .collect(toList());
        return new ParkingZoneInfo(parkingZones, parkingZonesEnabled);
    }

    private Point createPoint(EdgeWithParkingZones edge) {
        CoordinateXY coord = new CoordinateXY(edge.getFromVertex().getLon(), edge.getFromVertex().getLat());
        return new Point(new CoordinateArraySequence(new Coordinate[]{coord}), new GeometryFactory());
    }

    private SingleParkingZone findMatchingParkingZone(Point point, GeometryParkingZone geometryParkingZone) {
        if (isPointInParkingZone(point, geometryParkingZone)) {
            return getMatchingParkingZoneFromList(geometryParkingZone);
        } else {
            return null;
        }
    }

    private boolean isPointInParkingZone(Point point, GeometryParkingZone gpz) {
        return gpz.getGeometriesAllowed().stream().anyMatch(g -> g.contains(point))
                && gpz.getGeometriesDisallowed().stream().noneMatch(g -> g.contains(point));
    }

    private SingleParkingZone getMatchingParkingZoneFromList(GeometryParkingZone geometryParkingZone) {
        return parkingZonesEnabled.stream()
                .filter(pz -> pz.sameProviderIdAndVehicleType(geometryParkingZone))
                .findFirst()
                .orElse(null);
    }

    public void enableNewParkingZone(SingleParkingZone parkingZone, Graph graph) {
        if (!parkingZonesEnabled.contains(parkingZone)) {

            List<SingleParkingZone> newParkingZonesEnabled = new LinkedList<>(parkingZonesEnabled);
//          This should be thread secure because at any given time, only one updater may modify graph.
            newParkingZonesEnabled.add(parkingZone);

            parkingZonesEnabled = newParkingZonesEnabled;

            graph.getDropEdges().forEach(e -> e.setParkingZones(new ParkingZoneInfo(e.getParkingZones().getParkingZones(), parkingZonesEnabled)));
        }
    }

    public List<SingleParkingZone> getParkingZonesEnabled() {
        return parkingZonesEnabled;
    }
}
