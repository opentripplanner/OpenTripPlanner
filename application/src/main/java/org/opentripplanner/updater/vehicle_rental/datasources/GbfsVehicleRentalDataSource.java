package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSFreeBikeStatus;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v2_3.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStationStatus;
import org.mobilitydata.gbfs.v2_3.system_information.GBFSSystemInformation;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleTypes;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by demory on 2017-03-14.
 * <p>
 * Leaving OTPFeature.FloatingBike turned off both prevents floating bike updaters added to
 * router-config.json from being used, but more importantly, floating bikes added by a
 * VehicleRentalServiceDirectoryFetcher endpoint (which may be outside our control) will not be
 * used.
 */
class GbfsVehicleRentalDataSource implements VehicleRentalDataSource {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsVehicleRentalDataSource.class);

  private final GbfsVehicleRentalDataSourceParameters params;

  private final OtpHttpClient otpHttpClient;
  private GbfsFeedLoader loader;
  private List<GeofencingZone> geofencingZones = List.of();
  private boolean logGeofencingZonesDoesNotExistWarning = true;

  public GbfsVehicleRentalDataSource(
    GbfsVehicleRentalDataSourceParameters parameters,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    this.params = parameters;
    this.otpHttpClient = otpHttpClientFactory.create(LOG);
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
    final Map<String, RentalVehicleType> vehicleTypes = getVehicleTypes(system);

    List<VehicleRentalPlace> stations = new LinkedList<>();
    if (params.allowRentalType(RentalPickupType.STATION)) {
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
          params.allowKeepingRentedVehicleAtDestination(),
          params.overloadingAllowed()
        );

        // Iterate over all known stations, and if we have any status information add it to those station objects.
        stations.addAll(
          stationInformation
            .getData()
            .getStations()
            .stream()
            .map(stationInformationMapper::mapStationInformation)
            .filter(Objects::nonNull)
            .map(stationStatusMapper::mapStationStatus)
            .toList()
        );
      }
    }

    // Append the floating bike stations.
    if (OTPFeature.FloatingBike.isOn() && params.allowRentalType(RentalPickupType.FREE_FLOATING)) {
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
      if (zones != null) {
        var mapper = new GbfsGeofencingZoneMapper(system.systemId());
        this.geofencingZones = mapper.mapGeofencingZone(zones);
      } else {
        if (logGeofencingZonesDoesNotExistWarning) {
          LOG.warn(
            "GeofencingZones is enabled in OTP, but no zones exist for network: {}",
            params.network()
          );
        }
        logGeofencingZonesDoesNotExistWarning = false;
      }
    }
    return stations;
  }

  @Override
  public void setup() {
    loader = new GbfsFeedLoader(
      params.url(),
      params.httpHeaders(),
      params.language(),
      otpHttpClient
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(GbfsVehicleRentalDataSource.class)
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

  protected static Map<String, RentalVehicleType> mapVehicleTypes(
    GbfsVehicleTypeMapper vehicleTypeMapper,
    List<GBFSVehicleType> gbfsVehicleTypes
  ) {
    return gbfsVehicleTypes
      .stream()
      .map(vehicleTypeMapper::mapRentalVehicleType)
      .distinct()
      .collect(Collectors.toMap(v -> v.id().getId(), Function.identity()));
  }

  private Map<String, RentalVehicleType> getVehicleTypes(VehicleRentalSystem system) {
    GBFSVehicleTypes rawVehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
    if (rawVehicleTypes != null) {
      GbfsVehicleTypeMapper vehicleTypeMapper = new GbfsVehicleTypeMapper(system.systemId());
      List<GBFSVehicleType> gbfsVehicleTypes = rawVehicleTypes.getData().getVehicleTypes();
      return mapVehicleTypes(vehicleTypeMapper, gbfsVehicleTypes);
    }
    return Map.of();
  }
}
