package org.opentripplanner.updater.vehicle_rental.datasources;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleTypesAvailable;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsStationStatusMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsStationStatusMapper.class);

  private final Map<String, GBFSStation> statusLookup;
  private final Map<String, RentalVehicleType> vehicleTypes;

  public GbfsStationStatusMapper(
    Map<String, GBFSStation> statusLookup,
    Map<String, RentalVehicleType> vehicleTypes
  ) {
    this.statusLookup = Objects.requireNonNull(statusLookup);
    this.vehicleTypes = Objects.requireNonNull(vehicleTypes);
  }

  void fillStationStatus(VehicleRentalStation station) {
    if (!statusLookup.containsKey(station.getStationId())) {
      station.realTimeData = false;
      return;
    }
    GBFSStation status = statusLookup.get(station.getStationId());

    station.vehiclesAvailable = status.getNumBikesAvailable() != null
      ? status.getNumBikesAvailable()
      : 0;

    station.vehicleTypesAvailable = status.getVehicleTypesAvailable() != null
      ? status
        .getVehicleTypesAvailable()
        .stream()
        .filter(e -> containsVehicleType(e, status))
        .collect(Collectors.toMap(e -> vehicleTypes.get(e.getVehicleTypeId()), e -> e.getCount()))
      : Map.of(RentalVehicleType.getDefaultType(station.getNetwork()), station.vehiclesAvailable);

    station.vehiclesDisabled = status.getNumBikesDisabled() != null
      ? status.getNumBikesDisabled()
      : 0;

    station.spacesAvailable = status.getNumDocksAvailable() != null
      ? status.getNumDocksAvailable()
      : Integer.MAX_VALUE;

    station.vehicleSpacesAvailable = status.getVehicleDocksAvailable() != null
      ? status
        .getVehicleDocksAvailable()
        .stream()
        .flatMap(available ->
          available
            .getVehicleTypeIds()
            .stream()
            .map(t -> new VehicleTypeCount(vehicleTypes.get(t), available.getCount()))
        )
        .collect(Collectors.toMap(VehicleTypeCount::type, VehicleTypeCount::count))
      : Map.of(RentalVehicleType.getDefaultType(station.getNetwork()), station.spacesAvailable);

    station.spacesDisabled = status.getNumDocksDisabled() != null
      ? status.getNumDocksDisabled()
      : 0;

    station.isInstalled = status.getIsInstalled() != null ? status.getIsInstalled() : true;
    station.isRenting = status.getIsRenting() != null ? status.getIsRenting() : true;
    station.isReturning = status.getIsReturning() != null ? status.getIsReturning() : true;

    station.lastReported = status.getLastReported() != null
      ? Instant.ofEpochSecond(status.getLastReported().longValue())
      : null;
  }

  private boolean containsVehicleType(
    GBFSVehicleTypesAvailable vehicleTypesAvailable,
    GBFSStation station
  ) {
    boolean containsKey = vehicleTypes.containsKey(vehicleTypesAvailable.getVehicleTypeId());
    if (!containsKey) {
      LOG.warn(
        "Unexpected vehicle type ID {} in status for GBFS station {}",
        vehicleTypesAvailable.getVehicleTypeId(),
        station.getStationId()
      );
    }
    return containsKey;
  }

  private record VehicleTypeCount(RentalVehicleType type, int count) {}
}
