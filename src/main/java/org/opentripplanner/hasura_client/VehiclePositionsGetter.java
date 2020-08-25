package org.opentripplanner.hasura_client;

import org.opentripplanner.hasura_client.hasura_mappers.HasuraToOTPMapper;
import org.opentripplanner.hasura_client.hasura_mappers.VehiclePositionsMapper;
import org.opentripplanner.hasura_client.hasura_objects.Vehicle;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehiclePositionsGetter extends HasuraGetter<VehicleDescription, Vehicle> {

    private static final Logger LOG = LoggerFactory.getLogger(VehiclePositionsGetter.class);

    @Override
    protected String QUERY() {
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
                        "      \\n" +
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
    protected HasuraToOTPMapper<Vehicle, VehicleDescription> mapper() {
        return new VehiclePositionsMapper();

    }
}
