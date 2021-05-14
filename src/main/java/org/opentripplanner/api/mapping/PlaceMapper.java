package org.opentripplanner.api.mapping;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.api.model.ApiVehicleParkingWithEntrance;
import org.opentripplanner.api.model.ApiVehicleParkingWithEntrance.ApiVehicleParkingSpaces;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VehicleParkingWithEntrance;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

public class PlaceMapper {

    public static List<ApiPlace> mapStopArrivals(Collection<StopArrival> domain) {
        if(domain == null) { return null; }

        return domain.stream().map(PlaceMapper::mapStopArrival).collect(Collectors.toList());
    }

    public static ApiPlace mapStopArrival(StopArrival domain) {
        return mapPlace(domain.place, domain.arrival, domain.departure);
    }

    public static ApiPlace mapPlace(Place domain, Calendar arrival, Calendar departure) {
        if(domain == null) { return null; }

        ApiPlace api = new ApiPlace();

        api.name = domain.name;

        if (domain.stop != null) {
            api.stopId = FeedScopedIdMapper.mapToApi(domain.stop.getId());
            api.stopCode = domain.stop.getCode();
            api.platformCode = domain.stop instanceof Stop ? ((Stop) domain.stop).getPlatformCode() : null;
            api.zoneId = domain.stop instanceof Stop ? ((Stop) domain.stop).getFirstZoneAsString() : null;
        }

        if(domain.coordinate != null) {
            api.lon = domain.coordinate.longitude();
            api.lat = domain.coordinate.latitude();
        }

        api.arrival = arrival;
        api.departure = departure;
        api.orig = domain.orig;
        api.stopIndex = domain.stopIndex;
        api.stopSequence = domain.stopSequence;
        api.vertexType = VertexTypeMapper.mapVertexType(domain.vertexType);
        if (domain.vehicleRentalPlace != null) {
            api.bikeShareId = domain.vehicleRentalPlace.getStationId();
        }

        return api;
    }

    private static ApiVehicleParkingWithEntrance mapVehicleParking(VehicleParkingWithEntrance vehicleParkingWithEntrance) {
        var vp = vehicleParkingWithEntrance.getVehicleParking();
        var e = vehicleParkingWithEntrance.getEntrance();

        return ApiVehicleParkingWithEntrance.builder()
                .id(FeedScopedIdMapper.mapToApi(vp.getId()))
                .name(vp.getName().toString())
                .entranceId(FeedScopedIdMapper.mapToApi(e.getEntranceId()))
                .entranceName(vp.getName().toString())
                .detailsUrl(vp.getDetailsUrl())
                .imageUrl(vp.getImageUrl())
                .note(vp.getNote() != null ? vp.getNote().toString() : null)
                .tags(new ArrayList<>(vp.getTags()))
                .hasBicyclePlaces(vp.hasBicyclePlaces())
                .hasAnyCarPlaces(vp.hasAnyCarPlaces())
                .hasCarPlaces(vp.hasCarPlaces())
                .hasDisabledCarPlaces(vp.hasWheelchairAccessibledCarPlaces())
                .availability(mapVehicleParkingSpaces(vp.getAvailability()))
                .capacity(mapVehicleParkingSpaces(vp.getCapacity()))
                .build();
    }

    private static ApiVehicleParkingSpaces mapVehicleParkingSpaces(VehicleParkingSpaces parkingSpaces) {
        if (parkingSpaces == null) {
            return null;
        }

        return ApiVehicleParkingSpaces.builder()
                .bicycleSpaces(parkingSpaces.getBicycleSpaces())
                .carSpaces(parkingSpaces.getCarSpaces())
                .disabledCarSpaces(parkingSpaces.getWheelchairAccessibleCarSpaces())
                .build();
    }
}
