package org.opentripplanner.routing.vehicle_rental;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.I18NString;

public class VehicleRentalVehicle implements VehicleRentalPlace {

    public FeedScopedId id;
    public I18NString name;
    public double longitude;
    public double latitude;
    public boolean isCarStation = false;

    public VehicleRentalStationUris rentalUris;

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
    public boolean isAllowDropoff() {
        return false;
    }

    @Override
    public boolean isFloatingBike() {
        return true;
    }

    @Override
    public boolean isCarStation() {
        return isCarStation;
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
