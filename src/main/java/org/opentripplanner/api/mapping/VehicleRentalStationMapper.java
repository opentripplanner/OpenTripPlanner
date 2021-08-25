package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiVehicleRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.util.ArrayList;
import java.util.Locale;

public class VehicleRentalStationMapper {
    public static ApiVehicleRentalStation mapToApi(BikeRentalStation domain, Locale locale) {
        if(domain == null) { return null; }

        ApiVehicleRentalStation api = new ApiVehicleRentalStation();

        api.id = domain.id;
        api.name = domain.name.toString(locale);
        api.x = domain.longitude;
        api.y = domain.latitude;
        api.bikesAvailable = domain.bikesAvailable;
        api.spacesAvailable = domain.spacesAvailable;
        api.allowDropoff = domain.allowDropoff;
        api.isFloatingBike = domain.isFloatingBike;
        api.isCarStation = domain.isCarStation;
        api.networks = new ArrayList<>(domain.networks);
        api.realTimeData = domain.realTimeData;
        api.rentalUris = domain.rentalUris;

        return api;
    }
}
