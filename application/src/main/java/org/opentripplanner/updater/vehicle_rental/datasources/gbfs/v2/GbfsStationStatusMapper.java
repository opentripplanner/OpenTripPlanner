package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleTypesAvailable;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.ReturnPolicy;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GbfsStationStatusMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsStationStatusMapper.class);
  private static final Collector<
    VehicleTypeCount,
    ?,
    Map<RentalVehicleType, Integer>
  > TYPE_MAP_COLLECTOR = Collectors.toMap(VehicleTypeCount::type, VehicleTypeCount::count);

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

    var vehicleSpacesAvailable = vehicleSpaces(status);

    int spacesDisabled = status.getNumDocksDisabled() != null ? status.getNumDocksDisabled() : 0;

    boolean isRenting = toBoolean(status.getIsRenting());
    boolean isReturning = toBoolean(status.getIsReturning());

    return station
      .copyOf()
      .withVehiclesAvailable(vehiclesAvailable)
      .withVehicleTypesAvailable(vehicleTypesAvailable)
      .withVehiclesDisabled(vehiclesDisabled)
      .withSpacesAvailable(spacesAvailable)
      .withVehicleSpacesAvailable(vehicleSpacesAvailable)
      .withSpacesDisabled(spacesDisabled)
      .withIsRenting(isRenting)
      .withIsReturning(isReturning)
      .withReturnPolicy(returnPolicy(status))
      .build();
  }

  /**
   * Extract the available parking places from a variety of potentially incomplete information.
   */
  private Map<RentalVehicleType, Integer> vehicleSpaces(GBFSStation status) {
    var docksAvailable = status.getVehicleDocksAvailable();
    if (docksAvailable != null) {
      return docksAvailable
        .stream()
        .flatMap(available ->
          available
            .getVehicleTypeIds()
            .stream()
            .map(t -> new VehicleTypeCount(vehicleTypes.get(t), available.getCount()))
        )
        .collect(TYPE_MAP_COLLECTOR);
    } else {
      return Map.of();
    }
  }

  private static ReturnPolicy returnPolicy(GBFSStation station) {
    if (station.getVehicleDocksAvailable() == null) {
      return ReturnPolicy.ANY_TYPE;
    } else {
      return ReturnPolicy.SPECIFIC_TYPES;
    }
  }

  private static boolean toBoolean(@Nullable Boolean status) {
    return Boolean.TRUE.equals(status);
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
