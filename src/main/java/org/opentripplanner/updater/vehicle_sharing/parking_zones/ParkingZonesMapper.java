package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import com.google.gson.JsonObject;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesApiResponse.Feature;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesApiResponse.ParkingZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class ParkingZonesMapper {


    private static final Logger LOG = LoggerFactory.getLogger(ParkingZonesMapper.class);

    private final GeometryJSON geometryJSON = new GeometryJSON();

    private Geometry deserializeGeometry(JsonObject jsonObject) {
        try {
            return geometryJSON.read(jsonObject.toString());
        } catch (Exception e) {
            LOG.warn("Failed to deserialize GeometryJSON", e);
            return null;
        }
    }

    private List<Geometry> mapToGeometries(List<ParkingZone> parkingZones) {
        return parkingZones.stream()
                .map(ParkingZone::getPolygons)
                .flatMap(Collection::stream)
                .map(Feature::getGeometry)
                .map(this::deserializeGeometry)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private GeometryParkingZone mapToGeometryParkingZones(
            Map.Entry<String, List<ParkingZone>> vehicleTypeToParkingZones, int providerId
    ) {
        VehicleType vehicleType = VehicleType.fromString(vehicleTypeToParkingZones.getKey());
        if (vehicleType == null) {
            return null;
        }

        Map<Boolean, List<ParkingZone>> groupedByIsAllowed = vehicleTypeToParkingZones.getValue().stream()
                .collect(groupingBy(ParkingZone::isAllowed));
        List<Geometry> geometriesAllowed = mapToGeometries(groupedByIsAllowed.getOrDefault(true, emptyList()));
        List<Geometry> geometriesDisallowed = mapToGeometries(groupedByIsAllowed.getOrDefault(false, emptyList()));

        return new GeometryParkingZone(providerId, vehicleType, geometriesAllowed, geometriesDisallowed);
    }

    private List<GeometryParkingZone> mapToGeometryParkingZones(
            Map.Entry<Integer, List<ParkingZone>> providerIdToParkingZones
    ) {
        return providerIdToParkingZones.getValue().stream()
                .collect(groupingBy(ParkingZone::getVehicleType))
                .entrySet()
                .stream()
                .map(vehicleTypeToParkingZones -> mapToGeometryParkingZones(vehicleTypeToParkingZones,
                        providerIdToParkingZones.getKey()))
                .collect(toList());
    }

    public List<GeometryParkingZone> map(List<ParkingZone> parkingZones) {
        return parkingZones.stream()
                .collect(groupingBy(ParkingZone::getProviderId))
                .entrySet()
                .stream()
                .map(this::mapToGeometryParkingZones)
                .flatMap(Collection::stream)
                .collect(toList());
    }
}
