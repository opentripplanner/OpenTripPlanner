package org.opentripplanner.routing.core.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;

public class CarDescription extends VehicleDescription {

    private static final TraverseMode traverseMode = TraverseMode.CAR;

    private static final int rentTimeInSeconds = 60;

    private static final int dropoffTimeInSeconds = 60;

    public CarDescription(double longitude, double latitude, FuelType fuelType, Gearbox gearbox, Provider provider) {
        super(longitude, latitude, fuelType, gearbox, provider);
    }

    @Override
    public TraverseMode getTraverseMode() {
        return traverseMode;
    }

    @Override
    public int getRentTimeInSeconds() {
        return rentTimeInSeconds;
    }

    @Override
    public int getDropoffTimeInSeconds() {
        return dropoffTimeInSeconds;
    }
}
