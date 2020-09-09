package org.opentripplanner.routing.core.vehicle_sharing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.util.Objects;

public class KickScooterDescription extends BikePathVehicleDescription {
    protected static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_BIKEPATH = 15. * (10. / 36.);
    protected static final double MAX_SPEED_IN_METERS_PER_SECOND_ON_PEDESTRIAN_PATH = 10. * (10. / 36.);

    private static final TraverseMode TRAVERSE_MODE = TraverseMode.BICYCLE;

    private static final VehicleType VEHICLE_TYPE = VehicleType.KICKSCOOTER;

    private static final double DEFAULT_RANGE_IN_METERS = 16 * 1000;

    public KickScooterDescription(String providerVehicleId, double longitude, double latitude, FuelType fuelType,
                                  Gearbox gearbox, Provider provider, Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, provider, rangeInMeters);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KickScooterDescription(@JsonProperty("providerVehicleId") String providerVehicleId, @JsonProperty("longitude") double longitude,
                                  @JsonProperty("latitude") double latitude, @JsonProperty("fuelType") FuelType fuelType,
                                  @JsonProperty("gearbox") Gearbox gearbox, @JsonProperty("providerId") int providerId,
                                  @JsonProperty("providerName") String providerName, @JsonProperty("rangeInMeters") Double rangeInMeters) {
        super(providerVehicleId, longitude, latitude, fuelType, gearbox, new Provider(providerId, providerName), rangeInMeters);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KickScooterDescription that = (KickScooterDescription) o;
        return Objects.equals(getProviderVehicleId(), that.getProviderVehicleId()) &&
                Objects.equals(getProvider(), that.getProvider());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProviderVehicleId(), getProvider());
    }
}
