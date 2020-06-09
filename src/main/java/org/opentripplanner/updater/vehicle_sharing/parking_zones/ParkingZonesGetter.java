package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParkingZonesGetter {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingZonesGetter.class);

    private static final String QUERY =
            "{\"query\": \"query GetParkingZones {\n" +
                    "  parking_zones {\n" +
                    "    providerId\n" +
                    "    vehicleType\n" +
                    "    isAllowed\n" +
                    "    polygons\n" +
                    "  }" +
                    "}\"" +
                    "}";

    private final ParkingZonesMapper mapper = new ParkingZonesMapper();

    public List<GeometryParkingZone> getParkingZones(String url, Graph graph) {
        // TODO AdamWiktor add filtering parking zones for current graph to request
        ParkingZonesApiResponse response = HttpUtils.postData(url, QUERY, ParkingZonesApiResponse.class);
        LOG.info("Got parking zones from API");
        return mapper.map(response.getData().getParking_zones());
    }
}
