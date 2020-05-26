package org.opentripplanner.routing.core.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

import static java.lang.Double.min;

public class KickScooterDescription extends VehicleDescription {
    private static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_BIKEPATH = 15. * (10. / 36.);
    private static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_PEDESTRIAN_PATH = 10. * (10. / 36.);

    private static final TraverseMode TRAVERSE_MODE = TraverseMode.BICYCLE;

    private static final int RENT_TIME_IN_SECONDS = 30;

    private static final int DROPOFF_TIME_IN_SECONDS = 30;

    private static final VehicleType VEHICLE_TYPE = VehicleType.KICKSCOOTER;
    //  We don't want to return routes with long kickscooter legs.
    private static final double DEFAULT_RANGE_IN_METERS = 4 * 1000;

    public KickScooterDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                  Gearbox gearbox, Provider provider, Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, min(rangeInMeters, DEFAULT_RANGE_IN_METERS));
    }

    public KickScooterDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                  Gearbox gearbox, Provider provider) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider);
    }

    @Override
    public double getMaxSpeedInMetersPerSecond(StreetEdge streetEdge) {
        if (streetEdge.canTraverseIncludingBarrier(TraverseMode.BICYCLE))
            return MAX_SPEED_IN_METERS_PER_SECOND_ON_BIKEPATH;
        else
            return MAX_SPEED_IN_METERS_PER_SECOND_ON_PEDESTRIAN_PATH;
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

    @Override
    protected double getDefaultRangeInMeters() {
        return DEFAULT_RANGE_IN_METERS;
    }
}
