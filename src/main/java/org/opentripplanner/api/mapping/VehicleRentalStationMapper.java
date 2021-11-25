package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiVehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

import java.util.List;
import java.util.Locale;

public class VehicleRentalStationMapper {
    public static ApiVehicleRentalStation mapToApi(VehicleRentalPlace domain, Locale locale) {
        if(domain == null) { return null; }

        ApiVehicleRentalStation api = new ApiVehicleRentalStation();

        api.id = domain.getStationId();
        api.name = domain.getName().toString(locale);
        api.x = domain.getLongitude();
        api.y = domain.getLatitude();
        api.bikesAvailable = domain.getVehiclesAvailable();
        api.spacesAvailable = domain.getSpacesAvailable();
        api.allowDropoff = domain.isAllowDropoff();
        api.isFloatingBike = domain.isFloatingVehicle();
        api.isCarStation = domain.isCarStation();
        api.networks = List.of(domain.getNetwork());
        api.realTimeData = domain.isRealTimeData();
        api.rentalUris = domain.getRentalUris();

        return api;
    }
}
