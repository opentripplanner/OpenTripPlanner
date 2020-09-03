package org.opentripplanner.routing.core.vehicle_sharing;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

public class BikeDescription extends BikePathVehicleDescription {
    protected static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_BIKEPATH = 15. * (10. / 36.);
    protected static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_PEDESTRIAN_PATH = 10. * (10. / 36.);

    private static final VehicleType VEHICLE_TYPE = VehicleType.BIKE;
    private static final TraverseMode TRAVERSE_MODE = TraverseMode.BICYCLE;

    private static final double DEFAULT_RANGE_IN_METERS = 100 * 1000;

    public BikeDescription(String providerVehicleId, double longitude, double latitude, Provider provider) {
        super(providerVehicleId, longitude, latitude, null, null, provider);
        this.requiresHubToDrop = true;
    }

    public BikeDescription(BikeRentalStation station) {
        super(station.provider.getProviderName(), station.longitude, station.latitude, null, null, station == null ? null : station.provider);
        this.requiresHubToDrop = true;
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
    public VehicleType getVehicleType() {
        return VEHICLE_TYPE;
    }

    @Override
    protected double getDefaultRangeInMeters() {
        return DEFAULT_RANGE_IN_METERS;
    }

}
