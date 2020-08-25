package org.opentripplanner.hasura_client;

import org.opentripplanner.hasura_client.hasura_objects.ParkingZone;
import org.opentripplanner.hasura_client.mappers.HasuraToOTPMapper;
import org.opentripplanner.hasura_client.mappers.ParkingZonesMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.GeometryParkingZone;

public class ParkingZonesGetter extends HasuraGetter<GeometryParkingZone, ParkingZone> {
    @Override
    protected String QUERY() {
        return "{\"query\": \"query GetParkingZones {" +
                "  items:parking_zones {\\n" +
                "    providerId\\n" +
                "    vehicleType\\n" +
                "    isAllowed\\n" +
                "    area\\n" +
                "  }" +
                "}\"" +
                "}";
    }

    @Override
    protected String getGeolocationArguments(Graph graph) {
        return "";
    }

    @Override
    protected HasuraToOTPMapper<ParkingZone, GeometryParkingZone> mapper() {
        return new ParkingZonesMapper();
    }
}
