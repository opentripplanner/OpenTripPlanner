package org.opentripplanner.updater.car_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.car_rental.CarFuelType;
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Car2GoCarRentalDataSource extends GenericCarRentalDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(Car2GoCarRentalDataSource.class);

    @Override
    public void configure(Graph graph, JsonNode jsonNode) {
        this.networkName = "CAR2GO";
        super.configure(jsonNode);
    }

    protected ArrayList<CarRentalStation> parseVehicles(InputStream data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(data);
        JsonNode vehicles = rootNode.get("vehicles");
        ArrayList<CarRentalStation> stations = new ArrayList<>();
        if (vehicles.isArray()) {
            for (JsonNode vehicle : vehicles) {
                stations.add(makeStation(vehicle));
            }
        }
        return stations;
    }

    private CarRentalStation makeStation(JsonNode vehicle) {
        CarRentalStation car2go = new CarRentalStation();
        car2go.address = vehicle.get("address").asText();
        car2go.allowDropoff = false;
        car2go.allowPickup = vehicle.get("freeForRental").asBoolean();
        car2go.carsAvailable = 1;
        car2go.fuelType = CarFuelType.forValue(vehicle.get("fuelType").asText());
        car2go.id = vehicle.get("plate").asText();
        car2go.isFloatingCar = true;
        car2go.licensePlate = vehicle.get("plate").asText();
        car2go.name = new NonLocalizedString(vehicle.get("plate").asText());
        car2go.networks = new HashSet<>(Arrays.asList(networkName));
        car2go.spacesAvailable = 0;
        car2go.x = vehicle.path("geoCoordinate").path("longitude").asDouble();
        car2go.y = vehicle.path("geoCoordinate").path("latitude").asDouble();

        return car2go;
    }
}
