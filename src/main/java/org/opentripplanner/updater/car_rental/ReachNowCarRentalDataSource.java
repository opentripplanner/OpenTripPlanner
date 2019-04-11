package org.opentripplanner.updater.car_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.car_rental.CarFuelType;
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class ReachNowCarRentalDataSource extends GenericCarRentalDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(ReachNowCarRentalDataSource.class);

    @Override
    public void configure(Graph graph, JsonNode jsonNode) throws Exception {
        this.networkName = "REACHNOW";
        super.configure(jsonNode);
    }

    protected ArrayList<CarRentalStation> parseVehicles(InputStream data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(data);
        ArrayList<CarRentalStation> stations = new ArrayList<>();
        if (rootNode.isArray()) {
            for (JsonNode vehicle : rootNode) {
                stations.add(makeStation(vehicle));
            }
        }
        return stations;
    }

    private CarRentalStation makeStation(JsonNode vehicle) {
        CarRentalStation reachNowVehicle = new CarRentalStation();
        reachNowVehicle.address = vehicle.get("shortDisplayAddress").asText();
        reachNowVehicle.allowDropoff = false;
        reachNowVehicle.allowPickup = vehicle.get("available").asBoolean();
        reachNowVehicle.carsAvailable = 1;
        reachNowVehicle.fuelType = CarFuelType.forValue(vehicle.get("engineType").asText());
        reachNowVehicle.id = vehicle.get("id").asText();
        reachNowVehicle.isFloatingCar = true;
        reachNowVehicle.licensePlate = vehicle.get("licensePlate").asText();
        reachNowVehicle.name = new NonLocalizedString(vehicle.get("licensePlate").asText());
        reachNowVehicle.networks = new HashSet<>(Arrays.asList(networkName));
        reachNowVehicle.spacesAvailable = 0;
        reachNowVehicle.x = vehicle.path("location").path("lng").asDouble();
        reachNowVehicle.y = vehicle.path("location").path("lat").asDouble();

        return reachNowVehicle;
    }
}
