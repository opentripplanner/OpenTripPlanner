package org.opentripplanner.api.mapping;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.api.model.ApiVehicleParkingSpaces;
import org.opentripplanner.api.model.ApiVehicleParkingWithEntrance;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VehicleParkingWithEntrance;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;

public class PlaceMapper {

    private final Locale locale;

    public PlaceMapper(Locale locale) {
        this.locale = locale;
    }

    public List<ApiPlace> mapStopArrivals(Collection<StopArrival> domain) {
        if(domain == null) { return null; }

        return domain.stream().map(this::mapStopArrival).collect(Collectors.toList());
    }

    public ApiPlace mapStopArrival(StopArrival domain) {
        return mapPlace(
                domain.place,
                domain.arrival,
                domain.departure,
                domain.stopPosInPattern,
                domain.gtfsStopSequence
        );
    }

    public ApiPlace mapPlace(
            Place domain,
            Calendar arrival,
            Calendar departure,
            Integer stopIndex,
            Integer gtfsStopSequence
    ) {
        if(domain == null) { return null; }

        ApiPlace api = new ApiPlace();

        api.name = domain.name.toString(locale);

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
        api.stopIndex = stopIndex;
        api.stopSequence = gtfsStopSequence;
        api.vertexType = VertexTypeMapper.mapVertexType(domain.vertexType);
        if (domain.vehicleRentalPlace != null) {
            api.bikeShareId = domain.vehicleRentalPlace.getStationId();
        }
        if (domain.vehicleParkingWithEntrance != null) {
            api.vehicleParking = mapVehicleParking(domain.vehicleParkingWithEntrance);
        }

        return api;
    }

    private ApiVehicleParkingWithEntrance mapVehicleParking(VehicleParkingWithEntrance vehicleParkingWithEntrance) {
        var vp = vehicleParkingWithEntrance.getVehicleParking();
        var e = vehicleParkingWithEntrance.getEntrance();

        return ApiVehicleParkingWithEntrance.builder()
                .id(FeedScopedIdMapper.mapToApi(vp.getId()))
                .name(I18NStringMapper.mapToApi(vp.getName(), locale))
                .entranceId(FeedScopedIdMapper.mapToApi(e.getEntranceId()))
                .entranceName(I18NStringMapper.mapToApi(e.getName(), locale))
                .note(I18NStringMapper.mapToApi(vp.getNote(), locale))
                .detailsUrl(vp.getDetailsUrl())
                .imageUrl(vp.getImageUrl())
                .tags(new ArrayList<>(vp.getTags()))
                .hasBicyclePlaces(vp.hasBicyclePlaces())
                .hasAnyCarPlaces(vp.hasAnyCarPlaces())
                .hasCarPlaces(vp.hasCarPlaces())
                .hasWheelchairAccessibleCarPlaces(vp.hasWheelchairAccessibleCarPlaces())
                .availability(mapVehicleParkingSpaces(vp.getAvailability()))
                .capacity(mapVehicleParkingSpaces(vp.getCapacity()))
                .realtime(vehicleParkingWithEntrance.isRealtime())
                .build();
    }

    private static ApiVehicleParkingSpaces mapVehicleParkingSpaces(VehicleParkingSpaces parkingSpaces) {
        if (parkingSpaces == null) {
            return null;
        }

        return ApiVehicleParkingSpaces.builder()
                .bicycleSpaces(parkingSpaces.getBicycleSpaces())
                .carSpaces(parkingSpaces.getCarSpaces())
                .wheelchairAccessibleCarSpaces(parkingSpaces.getWheelchairAccessibleCarSpaces())
                .build();
    }
}
