package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.VehicleParkingWithEntrance;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiPlace;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiVehicleParkingSpaces;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiVehicleParkingWithEntrance;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.transit.model.site.RegularStop;

@Deprecated
class PlaceMapper {

  private final Locale locale;
  private final I18NStringMapper i18NStringMapper;

  public PlaceMapper(Locale locale) {
    this.locale = locale;
    i18NStringMapper = new I18NStringMapper(this.locale);
  }

  public List<ApiPlace> mapStopArrivals(Collection<StopArrival> domain) {
    if (domain == null) {
      return null;
    }

    return domain.stream().map(this::mapStopArrival).collect(Collectors.toList());
  }

  public ApiPlace mapStopArrival(StopArrival domain) {
    return mapPlace(
      domain.place,
      domain.arrival.time(),
      domain.departure.time(),
      domain.stopPosInPattern,
      domain.gtfsStopSequence
    );
  }

  public ApiPlace mapPlace(
    Place domain,
    ZonedDateTime arrival,
    ZonedDateTime departure,
    Integer stopIndex,
    Integer gtfsStopSequence
  ) {
    if (domain == null) {
      return null;
    }

    ApiPlace api = new ApiPlace();

    api.name = domain.name.toString(locale);

    if (domain.stop != null) {
      api.stopId = FeedScopedIdMapper.mapToApi(domain.stop.getId());
      api.stopCode = domain.stop.getCode();
      api.platformCode = domain.stop instanceof RegularStop
        ? ((RegularStop) domain.stop).getPlatformCode()
        : null;
      api.zoneId = domain.stop instanceof RegularStop
        ? ((RegularStop) domain.stop).getFirstZoneAsString()
        : null;
    }

    if (domain.coordinate != null) {
      api.lon = domain.coordinate.longitude();
      api.lat = domain.coordinate.latitude();
    }

    api.arrival = Optional.ofNullable(arrival).map(GregorianCalendar::from).orElse(null);
    api.departure = Optional.ofNullable(departure).map(GregorianCalendar::from).orElse(null);
    api.stopIndex = stopIndex;
    api.stopSequence = gtfsStopSequence;
    api.vertexType = VertexTypeMapper.mapVertexType(domain.vertexType);
    if (domain.vehicleRentalPlace != null) {
      api.bikeShareId = domain.vehicleRentalPlace.getStationId();
      // for backwards-compatibility with the IBI frontend this always returns a list of a single item
      api.networks = List.of(domain.vehicleRentalPlace.getNetwork());
    }
    if (domain.vehicleParkingWithEntrance != null) {
      api.vehicleParking = mapVehicleParking(domain.vehicleParkingWithEntrance);
    }

    return api;
  }

  private static ApiVehicleParkingSpaces mapVehicleParkingSpaces(
    VehicleParkingSpaces parkingSpaces
  ) {
    if (parkingSpaces == null) {
      return null;
    }

    return ApiVehicleParkingSpaces.builder()
      .bicycleSpaces(parkingSpaces.getBicycleSpaces())
      .carSpaces(parkingSpaces.getCarSpaces())
      .wheelchairAccessibleCarSpaces(parkingSpaces.getWheelchairAccessibleCarSpaces())
      .build();
  }

  private ApiVehicleParkingWithEntrance mapVehicleParking(
    VehicleParkingWithEntrance vehicleParkingWithEntrance
  ) {
    var vp = vehicleParkingWithEntrance.getVehicleParking();
    var e = vehicleParkingWithEntrance.getEntrance();

    return ApiVehicleParkingWithEntrance.builder()
      .id(FeedScopedIdMapper.mapToApi(vp.getId()))
      .name(i18NStringMapper.mapToApi(vp.getName()))
      .entranceId(FeedScopedIdMapper.mapToApi(e.getEntranceId()))
      .entranceName(i18NStringMapper.mapToApi(e.getName()))
      .note(i18NStringMapper.mapToApi(vp.getNote()))
      .detailsUrl(vp.getDetailsUrl())
      .imageUrl(vp.getImageUrl())
      .tags(new ArrayList<>(vp.getTags()))
      .hasBicyclePlaces(vp.hasBicyclePlaces())
      .hasAnyCarPlaces(vp.hasAnyCarPlaces())
      .hasCarPlaces(vp.hasCarPlaces())
      .hasWheelchairAccessibleCarPlaces(vp.hasWheelchairAccessibleCarPlaces())
      .availability(mapVehicleParkingSpaces(vp.getAvailability()))
      .capacity(mapVehicleParkingSpaces(vp.getCapacity()))
      .realTime(vehicleParkingWithEntrance.isRealtime())
      .build();
  }
}
