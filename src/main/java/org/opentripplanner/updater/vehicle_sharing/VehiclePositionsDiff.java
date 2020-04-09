package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VehiclePositionsDiff {

    private static Logger LOG = LoggerFactory.getLogger(VehiclePositionsDiff.class);

    private static final String CAR = "car";
    private static final String MOTORBIKE = "scooter";

    private final List<VehicleDescription> appeared;

    public VehiclePositionsDiff(List<SharedVehiclesApiResponse.Vehicle> vehicles) {
        this.appeared = vehicles.stream().map(this::mapper).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private VehicleDescription mapper(SharedVehiclesApiResponse.Vehicle vehicle) {
        long id = vehicle.getId();
        double longitude = vehicle.getLongitude();
        double latitude = vehicle.getLatitude();
        FuelType fuelType = FuelType.fromString(vehicle.getFuelType());
        Gearbox gearbox = Gearbox.fromString(vehicle.getGearbox());
        Provider provider = Provider.fromId(vehicle.getProviderId());
        switch (vehicle.getType()) {
            case CAR:
                return new CarDescription(id, longitude, latitude, fuelType, gearbox, provider);
            case MOTORBIKE:
                return new MotorbikeDescription(id, longitude, latitude, fuelType, gearbox, provider);
            default: // TODO (AdamWiktor) Add support for kickscooter
                LOG.warn("Omitting vehicle {} because of unsupported type {}", id, vehicle.getType());
                return null;
        }
    }

    public List<VehicleDescription> getAppeared() {
        return appeared;
    }
}
