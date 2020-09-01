package org.opentripplanner.hasura_client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.opentripplanner.hasura_client.hasura_objects.Vehicle;
import org.opentripplanner.hasura_client.mappers.HasuraToOTPMapper;
import org.opentripplanner.hasura_client.mappers.VehiclePositionsMapper;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehiclePositionsGetter extends HasuraGetter<VehicleDescription, Vehicle> {
    private static final Logger LOG = LoggerFactory.getLogger(VehiclePositionsGetter.class);

    @Override
    protected String query() {
        return
                "{\"query\": \"query VehiclesForArea($latMin: float8, $lonMin: float8, $latMax: float8, $lonMax: float8) {\\n" +
                        "  items:vehicles(\\n" +
                        "    where: {\\n" +
                        "    latitude: {\\n" +
                        "      _gte: $latMin\\n" +
                        "      _lte: $latMax\\n" +
                        "    }\\n" +
                        "    longitude: {\\n" +
                        "      _gte: $lonMin\\n" +
                        "      _lte: $lonMax\\n" +
                        "    }\\n" +
                        "    provider:{\\n" +
                        "      available : {\\n" +
                        "        \t_eq: true\\n" +
                        "      }\\n" +
                        "    }\\n" +
                        "  }) {\\n" +
                        "    providerVehicleId\\n" +
                        "    latitude\\n" +
                        "    longitude\\n" +
                        "    fuelType\\n" +
                        "    gearbox\\n" +
                        "    type\\n" +
                        "    range\\n" +
                        "    provider {\\n" +
                        "      id\\n" +
                        "      name\\n" +
                        "      available\\n" +
                        "    }\\n" +
                        "  }\\n" +
                        "}\",";

    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected HasuraToOTPMapper<Vehicle, VehicleDescription> mapper() {
        return new VehiclePositionsMapper();
    }

    @Override
    protected TypeReference<ApiResponse<Vehicle>> hasuraType() {
        return new TypeReference<ApiResponse<Vehicle>>() {
        };
    }
}
