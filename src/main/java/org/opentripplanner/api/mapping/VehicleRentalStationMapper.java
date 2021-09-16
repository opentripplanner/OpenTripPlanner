package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiVehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

import java.util.List;
import java.util.Locale;

public class VehicleRentalStationMapper {
    public static ApiVehicleRentalStation mapToApi(VehicleRentalStation domain, Locale locale) {
        if(domain == null) { return null; }

        ApiVehicleRentalStation api = new ApiVehicleRentalStation();

        api.id = domain.getStationId();
        api.name = domain.name.toString(locale);
        api.x = domain.longitude;
        api.y = domain.latitude;
        api.bikesAvailable = domain.vehiclesAvailable;
        api.spacesAvailable = domain.spacesAvailable;
        api.allowDropoff = domain.allowDropoff;
        api.isFloatingBike = domain.isFloatingBike;
        api.isCarStation = domain.isCarStation;
        api.networks = List.of(domain.getNetwork());
        api.realTimeData = domain.realTimeData;
        api.rentalUris = domain.rentalUris;

        return api;
    }
}
