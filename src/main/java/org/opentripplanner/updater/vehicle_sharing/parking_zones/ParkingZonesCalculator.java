package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class ParkingZonesCalculator {

    private final List<GeometryParkingZone> geometryParkingZones;

    public ParkingZonesCalculator(List<GeometryParkingZone> geometryParkingZones) {
        this.geometryParkingZones = geometryParkingZones;
    }

    public List<SingleParkingZone> getNewParkingZonesEnabled() {
        return geometryParkingZones.stream()
                .map(gpz -> new SingleParkingZone(gpz.getProviderId(), gpz.getVehicleType()))
                .distinct()
                .collect(toList());
    }

    public List<SingleParkingZone> getParkingZonesForRentEdge(RentVehicleAnywhereEdge edge,
                                                              List<SingleParkingZone> parkingZonesEnabled) {
        Point point = createPoint(edge);
        return geometryParkingZones.stream()
                .map(gpz -> findMatchingParkingZone(point, gpz, parkingZonesEnabled))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Point createPoint(RentVehicleAnywhereEdge e) {
        CoordinateXY coord = new CoordinateXY(e.getFromVertex().getLon(), e.getFromVertex().getLat());
        return new Point(new CoordinateArraySequence(new Coordinate[]{coord}), new GeometryFactory());
    }

    private SingleParkingZone findMatchingParkingZone(Point point, GeometryParkingZone geometryParkingZone,
                                                      List<SingleParkingZone> parkingZonesEnabled) {
        if (isPointInParkingZone(point, geometryParkingZone)) {
            return getMatchingParkingZoneFromList(geometryParkingZone, parkingZonesEnabled);
        } else {
            return null;
        }
    }

    private boolean isPointInParkingZone(Point point, GeometryParkingZone gpz) {
        return gpz.getGeometriesAllowed().stream().anyMatch(g -> g.contains(point))
                && gpz.getGeometriesDisallowed().stream().noneMatch(g -> g.contains(point));
    }

    private SingleParkingZone getMatchingParkingZoneFromList(
            GeometryParkingZone geometryParkingZone, List<SingleParkingZone> parkingZonesEnabled) {
        return parkingZonesEnabled.stream()
                .filter(pz -> pz.sameProviderIdAndVehicleType(geometryParkingZone))
                .findFirst()
                .orElse(null);
    }
}
