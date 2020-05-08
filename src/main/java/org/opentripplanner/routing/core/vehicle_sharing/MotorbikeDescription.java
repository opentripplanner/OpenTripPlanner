package org.opentripplanner.routing.core.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

import static java.lang.Double.min;

public class MotorbikeDescription extends VehicleDescription {

    private static final double MAX_SPEED_IN_METERS_PER_SECOND = 12;

    private static final TraverseMode TRAVERSE_MODE = TraverseMode.CAR;

    private static final int RENT_TIME_IN_SECONDS = 60;

    private static final int DROPOFF_TIME_IN_SECONDS = 60 * 2;

    private static final VehicleType VEHICLE_TYPE = VehicleType.MOTORBIKE;

    public MotorbikeDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                Gearbox gearbox, Provider provider) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider);
    }

    @Override
    public double getMaxSpeedInMetersPerSecond(StreetEdge streetEdge) {
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
