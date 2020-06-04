package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VehiclePositionsDiff {

    private static final Logger LOG = LoggerFactory.getLogger(VehiclePositionsDiff.class);

    private static final String CAR = "car";
    private static final String MOTORBIKE = "scooter";
    private static final String KICKSCOOTER = "un-pedal-scooter";

    private final List<VehicleDescription> appeared;

    public VehiclePositionsDiff(List<SharedVehiclesApiResponse.Vehicle> vehicles) {
        this.appeared = vehicles.stream().map(this::mapper).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private VehicleDescription mapper(SharedVehiclesApiResponse.Vehicle vehicle) {
        if (vehicle.getProvider() == null) {
            LOG.warn("Omitting vehicle {} because of lack of provider", vehicle.getProviderVehicleId());
            return null;
        }
        String providerVehicleId = vehicle.getProviderVehicleId();
        double longitude = vehicle.getLongitude();
        double latitude = vehicle.getLatitude();
        FuelType fuelType = FuelType.fromString(vehicle.getFuelType());
        Gearbox gearbox = Gearbox.fromString(vehicle.getGearbox());
        Provider provider = new Provider(vehicle.getProvider().getId(), vehicle.getProvider().getName());
        Double rangeInMeters = vehicle.getRangeInMeters();
        VehicleType vehicleType = VehicleType.fromString(vehicle.getType());
        if (vehicleType == null) {
            LOG.warn("Omitting vehicle {} because of unsupported type {}", providerVehicleId, vehicle.getType());
            return null;
        }
        switch (vehicleType) {
            case CAR:
                return new CarDescription(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
            case MOTORBIKE:
                return new MotorbikeDescription(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
            case KICKSCOOTER:
                return new KickScooterDescription(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
            default:
                // this should never happen
                LOG.warn("Omitting vehicle {} because of unsupported type {}", providerVehicleId, vehicleType);
                return null;
        }
    }

    public List<VehicleDescription> getAppeared() {
        return appeared;
    }
}
