package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entur.gbfs.v2_3.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_3.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_3.station_status.GBFSStation;
import org.entur.gbfs.v2_3.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_3.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_3.vehicle_types.GBFSVehicleTypes;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

/**
 * Created by demory on 2017-03-14.
 * <p>
 * Leaving OTPFeature.FloatingBike turned off both prevents floating bike updaters added to
 * router-config.json from being used, but more importantly, floating bikes added by a
 * VehicleRentalServiceDirectoryFetcher endpoint (which may be outside our control) will not be
 * used.
 */
class GbfsVehicleRentalDataSource implements VehicleRentalDatasource {

  private final GbfsVehicleRentalDataSourceParameters params;

  private GbfsFeedLoader loader;
  private List<GeofencingZone> geofencingZones = List.of();

  public GbfsVehicleRentalDataSource(GbfsVehicleRentalDataSourceParameters parameters) {
    this.params = parameters;
  }

  @Override
  public boolean update() {
    if (loader == null) {
      return false;
    }
    return loader.update();
  }

  @Override
  public List<VehicleRentalPlace> getUpdates() {
    // Get system information
    GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
    GbfsSystemInformationMapper systemInformationMapper = new GbfsSystemInformationMapper();
    VehicleRentalSystem system = systemInformationMapper.mapSystemInformation(
      systemInformation.getData(),
      params.network()
    );

    // Get vehicle types
    Map<String, RentalVehicleType> vehicleTypes = null;
    GBFSVehicleTypes rawVehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
    if (rawVehicleTypes != null) {
      GbfsVehicleTypeMapper vehicleTypeMapper = new GbfsVehicleTypeMapper(system.systemId);
      vehicleTypes =
        rawVehicleTypes
          .getData()
          .getVehicleTypes()
          .stream()
          .map(vehicleTypeMapper::mapRentalVehicleType)
          .collect(Collectors.toMap(v -> v.id.getId(), Function.identity()));
    }

    List<VehicleRentalPlace> stations = new LinkedList<>();

    // Both station information and status are required for all systems using stations
    GBFSStationInformation stationInformation = loader.getFeed(GBFSStationInformation.class);
    GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
    if (stationInformation != null && stationStatus != null) {
      // Index all the station status entries on their station ID.
      Map<String, GBFSStation> statusLookup = stationStatus
        .getData()
        .getStations()
        .stream()
        .collect(Collectors.toMap(GBFSStation::getStationId, Function.identity()));
      GbfsStationStatusMapper stationStatusMapper = new GbfsStationStatusMapper(
        statusLookup,
        vehicleTypes
      );
      GbfsStationInformationMapper stationInformationMapper = new GbfsStationInformationMapper(
        system,
        vehicleTypes,
        params.allowKeepingRentedVehicleAtDestination()
      );

      // Iterate over all known stations, and if we have any status information add it to those station objects.
      stations.addAll(
        stationInformation
          .getData()
          .getStations()
          .stream()
          .map(stationInformationMapper::mapStationInformation)
          .filter(Objects::nonNull)
          .peek(stationStatusMapper::fillStationStatus)
          .toList()
      );
    }

    // Append the floating bike stations.
    if (OTPFeature.FloatingBike.isOn()) {
      GBFSFreeBikeStatus freeBikeStatus = loader.getFeed(GBFSFreeBikeStatus.class);
      if (freeBikeStatus != null) {
        GbfsFreeVehicleStatusMapper freeVehicleStatusMapper = new GbfsFreeVehicleStatusMapper(
          system,
          vehicleTypes
        );
        stations.addAll(
          freeBikeStatus
            .getData()
            .getBikes()
            .stream()
            .map(freeVehicleStatusMapper::mapFreeVehicleStatus)
            .filter(Objects::nonNull)
            .toList()
        );
      }
    }

    if (params.geofencingZones()) {
      var zones = loader.getFeed(GBFSGeofencingZones.class);

      var mapper = new GbfsGeofencingZoneMapper(system.systemId);
      this.geofencingZones = mapper.mapGeofencingZone(zones);
    }
    return stations;
  }

  @Override
  public void setup() {
    loader = new GbfsFeedLoader(params.url(), params.httpHeaders(), params.language());
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(GbfsVehicleRentalDataSource.class)
      .addStr("url", params.url())
      .addStr("language", params.language())
      .addBoolIfTrue(
        "allowKeepingRentedVehicleAtDestination",
        params.allowKeepingRentedVehicleAtDestination()
      )
      .toString();
  }

  @Override
  public List<GeofencingZone> getGeofencingZones() {
    return this.geofencingZones;
  }
}
