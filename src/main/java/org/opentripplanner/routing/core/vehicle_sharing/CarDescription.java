package org.opentripplanner.routing.core.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;

public class CarDescription extends VehicleDescription {

    private static final double MAX_SPEED_IN_METERS_PER_SECOND = 40;

    private static final TraverseMode TRAVERSE_MODE = TraverseMode.CAR;

    private static final int RENT_TIME_IN_SECONDS = 90;

    private static final int DROPOFF_TIME_IN_SECONDS = 90;

    private static final VehicleType VEHICLE_TYPE = VehicleType.CAR;

    public CarDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                          Gearbox gearbox, Provider provider) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider);
    }

    @Override
    public double getMaxSpeedInMetersPerSecond() {
        return MAX_SPEED_IN_METERS_PER_SECOND;
    }

    @Override
    public TraverseMode getTraverseMode() {
        return TRAVERSE_MODE;
    }

    @Override
    public int getRentTimeInSeconds() {
        return RENT_TIME_IN_SECONDS;
    }

    @Override
    public int getDropoffTimeInSeconds() {
        return DROPOFF_TIME_IN_SECONDS;
    }

    @Override
    public VehicleType getVehicleType() {
        return VEHICLE_TYPE;
    }
}
