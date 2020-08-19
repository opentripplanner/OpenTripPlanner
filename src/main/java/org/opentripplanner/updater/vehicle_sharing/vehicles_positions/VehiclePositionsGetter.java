package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VehiclePositionsGetter {

    private static final Logger LOG = LoggerFactory.getLogger(VehiclePositionsGetter.class);

    private static final String QUERY =
            "{\"query\": \"query VehiclesForArea($latMin: float8, $lonMin: float8, $latMax: float8, $lonMax: float8) {\\n" +
                    "  vehicles(\\n" +
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

    private final VehiclePositionsMapper mapper = new VehiclePositionsMapper();

    private String getArguments(Graph graph) {
        double latMin = graph.getEnvelope().getLowerLeftLatitude();
        double lonMin = graph.getEnvelope().getLowerLeftLongitude();
        double latMax = graph.getEnvelope().getUpperRightLatitude();
        double lonMax = graph.getEnvelope().getUpperRightLongitude();
        return "\"variables\": {" +
                "  \"latMin\": " + latMin + "," +
                "  \"lonMin\": " + lonMin + "," +
                "  \"latMax\": " + latMax + "," +
                "  \"lonMax\": " + lonMax +
                "}}";
    }

    public List<VehicleDescription> getVehicleDescriptions(Graph graph, String url) {
        String arguments = getArguments(graph);
        String body = QUERY + arguments;
        SharedVehiclesApiResponse response = HttpUtils.postData(url, body, SharedVehiclesApiResponse.class);
        LOG.info("Got {} vehicles from API", response.getData().getVehicles().size());
        return mapper.map(response.getData().getVehicles());
    }
}
