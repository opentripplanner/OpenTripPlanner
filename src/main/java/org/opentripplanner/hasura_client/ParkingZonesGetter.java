package org.opentripplanner.hasura_client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.opentripplanner.hasura_client.hasura_objects.ParkingZone;
import org.opentripplanner.hasura_client.mappers.HasuraToOTPMapper;
import org.opentripplanner.hasura_client.mappers.ParkingZonesMapper;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.GeometryParkingZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParkingZonesGetter extends HasuraGetter<GeometryParkingZone, ParkingZone> {
    private static final Logger LOG = LoggerFactory.getLogger(HasuraGetter.class);

    @Override
    protected String query() {
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
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected boolean addGeolocationArguments() {
        return false;
    }

    @Override
    protected HasuraToOTPMapper<ParkingZone, GeometryParkingZone> mapper() {
        return new ParkingZonesMapper();
    }

    @Override
    protected TypeReference<ApiResponse<ParkingZone>> hasuraType() {
        return new TypeReference<ApiResponse<ParkingZone>>() {
        };
    }


}
