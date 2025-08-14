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

  VehicleRentalStation mapStationStatus(VehicleRentalStation station) {
    if (!statusLookup.containsKey(station.stationId())) {
      return station.copyOf().withRealTimeData(false).build();
    }
    GBFSStation status = statusLookup.get(station.stationId());

    int vehiclesAvailable = status.getNumBikesAvailable() != null
      ? status.getNumBikesAvailable()
      : 0;

    Map<RentalVehicleType, Integer> vehicleTypesAvailable = status.getVehicleTypesAvailable() !=
      null
      ? status
        .getVehicleTypesAvailable()
        .stream()
        .filter(e -> containsVehicleType(e, status))
        .collect(Collectors.toMap(e -> vehicleTypes.get(e.getVehicleTypeId()), e -> e.getCount()))
      : Map.of(RentalVehicleType.getDefaultType(station.network()), vehiclesAvailable);

    int vehiclesDisabled = status.getNumBikesDisabled() != null ? status.getNumBikesDisabled() : 0;

    int spacesAvailable = status.getNumDocksAvailable() != null
      ? status.getNumDocksAvailable()
      : Integer.MAX_VALUE;

    Map<RentalVehicleType, Integer> vehicleSpacesAvailable = status.getVehicleDocksAvailable() !=
      null
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
      : Map.of(RentalVehicleType.getDefaultType(station.network()), spacesAvailable);

    int spacesDisabled = status.getNumDocksDisabled() != null ? status.getNumDocksDisabled() : 0;

    boolean isInstalled = status.getIsInstalled() != null ? status.getIsInstalled() : true;
    boolean isRenting = status.getIsRenting() != null ? status.getIsRenting() : true;
    boolean isReturning = status.getIsReturning() != null ? status.getIsReturning() : true;

    Instant lastReported = status.getLastReported() != null
      ? Instant.ofEpochSecond(status.getLastReported().longValue())
      : null;

    return station
      .copyOf()
      .withVehiclesAvailable(vehiclesAvailable)
      .withVehicleTypesAvailable(vehicleTypesAvailable)
      .withVehiclesDisabled(vehiclesDisabled)
      .withSpacesAvailable(spacesAvailable)
      .withVehicleSpacesAvailable(vehicleSpacesAvailable)
      .withSpacesDisabled(spacesDisabled)
      .withIsInstalled(isInstalled)
      .withIsRenting(isRenting)
      .withIsReturning(isReturning)
      .withLastReported(lastReported)
      .build();
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
