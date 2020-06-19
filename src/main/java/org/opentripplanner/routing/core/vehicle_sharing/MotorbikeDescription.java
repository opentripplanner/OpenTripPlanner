package org.opentripplanner.routing.core.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

public class MotorbikeDescription extends VehicleDescription {

    private static final double DEFAULT_RANGE_IN_METERS = 50 * 1000;
    private static final double MAX_SPEED_IN_METERS_PER_SECOND = 12;

    private static final TraverseMode TRAVERSE_MODE = TraverseMode.CAR;

    private static final VehicleType VEHICLE_TYPE = VehicleType.MOTORBIKE;

    public MotorbikeDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                Gearbox gearbox, Provider provider, Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
    }

    public MotorbikeDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                Gearbox gearbox, Provider provider) {
        this(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, DEFAULT_RANGE_IN_METERS);
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
    public VehicleType getVehicleType() {
        return VEHICLE_TYPE;
    }

    @Override
    protected double getDefaultRangeInMeters() {
        return DEFAULT_RANGE_IN_METERS;
    }
}
