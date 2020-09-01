package org.opentripplanner.routing.core.vehicle_sharing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opentripplanner.routing.core.TraverseMode;

public abstract class KickScooterOrBikeDescription extends VehicleDescription {
    private static final TraverseMode TRAVERSE_MODE = TraverseMode.BICYCLE;


    protected static final double DEFAULT_RANGE_IN_METERS = 16 * 1000;

    public KickScooterOrBikeDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                        Gearbox gearbox, Provider provider, Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KickScooterOrBikeDescription(@JsonProperty("providerVehicleId") String providerVehicleId, @JsonProperty("longitude") double longitude,
                                        @JsonProperty("latitude") double latitude, @JsonProperty("fuelType") FuelType fuelType,
                                        @JsonProperty("gearbox") Gearbox gearbox, @JsonProperty("providerId") int providerId,
                                        @JsonProperty("providerName") String providerName, @JsonProperty("rangeInMeters") Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, new Provider(providerId, providerName), rangeInMeters);
    }


    public KickScooterOrBikeDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                        Gearbox gearbox, Provider provider) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider);
    }


    @Override
    public TraverseMode getTraverseMode() {
        return TRAVERSE_MODE;
    }


    @Override
    protected double getDefaultRangeInMeters() {
        return DEFAULT_RANGE_IN_METERS;
    }

    @Override
    protected Double getMaximumRangeInMeters() {
        return getDefaultRangeInMeters();
    }
}
