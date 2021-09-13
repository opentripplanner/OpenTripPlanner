package org.opentripplanner.routing.vehicle_rental;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.I18NString;

public interface VehicleRentalPlace {

    FeedScopedId getId();

    String getStationId();

    String getNetwork();

    I18NString getName();

    double getLongitude();

    double getLatitude();

    int getVehiclesAvailable();

    int getSpacesAvailable();

    boolean isAllowDropoff();

    boolean isRenting();

    boolean isFloatingBike();

    boolean isCarStation();

    boolean isKeepingVehicleRentalAtDestinationAllowed();

    boolean isRealTimeData();

    VehicleRentalStationUris getRentalUris();
}
