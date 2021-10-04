package org.opentripplanner.routing.vehicle_rental;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.I18NString;

import java.time.ZonedDateTime;

public class VehicleRentalVehicle implements VehicleRentalPlace {

    public FeedScopedId id;
    public I18NString name;
    public double longitude;
    public double latitude;

    public VehicleRentalSystem system;
    public RentalVehicleType vehicleType;
    public VehicleRentalStationUris rentalUris;
    public boolean isReserved = false;
    public boolean isDisabled = false;
    public ZonedDateTime lastReported;
    public Double currentRangeMeters;
    public VehicleRentalStation station;
    public String pricingPlanId;


    @Override
    public FeedScopedId getId() {
        return id;
    }

    @Override
    public String getStationId() {
        return getId().getId();
    }

    @Override
    public String getNetwork() {
        return getId().getFeedId();
    }

    @Override
    public I18NString getName() {
        return name;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public int getVehiclesAvailable() {
        return 1;
    }

    @Override
    public int getSpacesAvailable() {
        return 0;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public boolean isAllowDropoff() {
        return false;
    }

    @Override
    public boolean isAllowOverloading() {
        return false;
    }

    @Override
    public boolean isAllowPickup() {
        return false;
    }

    public boolean allowPickupNow() {
        return !isReserved && !isDisabled;
    }

    public boolean allowDropoffNow() {
        return false;
    }

    @Override
    public boolean isFloatingBike() {
        return true;
    }

    @Override
    public boolean isCarStation() {
        return vehicleType.formFactor.equals(RentalVehicleType.FormFactor.CAR);
    }

    @Override
    public boolean isKeepingVehicleRentalAtDestinationAllowed() {
        return false;
    }

    @Override
    public boolean isRealTimeData() {
        return true;
    }

    @Override
    public VehicleRentalStationUris getRentalUris() {
        return rentalUris;
    }
}
