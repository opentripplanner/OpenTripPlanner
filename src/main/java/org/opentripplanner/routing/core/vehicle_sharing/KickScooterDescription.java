package org.opentripplanner.routing.core.vehicle_sharing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

public class KickScooterDescription extends VehicleDescription {
    private static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_BIKEPATH = 15. * (10. / 36.);
    private static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_PEDESTRIAN_PATH = 10. * (10. / 36.);

    private static final TraverseMode TRAVERSE_MODE = TraverseMode.BICYCLE;

    private static final VehicleType VEHICLE_TYPE = VehicleType.KICKSCOOTER;

    private static final double DEFAULT_RANGE_IN_METERS = 16 * 1000;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KickScooterDescription(@JsonProperty("providerVehicleId") String providerVehicleId, @JsonProperty("longitude") double longitude,
                                  @JsonProperty("latitude") double latitude, @JsonProperty("fuelType") FuelType fuelType,
                                  @JsonProperty("gearbox") Gearbox gearbox, @JsonProperty("provider") Provider provider,
                                  @JsonProperty("rangeInMeters") Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
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
    public VehicleType getVehicleType() {
        return VEHICLE_TYPE;
    }

    @Override
    protected double getDefaultRangeInMeters() {
        return DEFAULT_RANGE_IN_METERS;
    }

    //  We don't want to return routes with long kickscooter legs.
    @Override
    protected Double getMaximumRangeInMeters() {
        return getDefaultRangeInMeters();
    }
}
