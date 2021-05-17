package org.opentripplanner.updater.vehicle_parking;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;

public class BicycleParkAPIUpdater extends ParkAPIUpdater {

    public BicycleParkAPIUpdater(
            String url,
            String feedId,
            Map<String, String> httpHeaders
    ) {
        super(url, feedId, httpHeaders);
    }

    @Override
    protected VehicleParkingSpaces parseCapacity(JsonNode jsonNode) {
        return parseVehicleSpaces(jsonNode, "total", null, null);
    }

    @Override
    protected VehicleParkingSpaces parseAvailability(JsonNode jsonNode) {
        return parseVehicleSpaces(jsonNode, "free", null, null);
    }
}
