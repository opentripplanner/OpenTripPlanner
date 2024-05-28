package org.opentripplanner.updater.vehicle_rental.datasources;

import static java.util.Objects.requireNonNullElse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSBike;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSRentalUris;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class GbfsFreeVehicleStatusMapper {

  private final VehicleRentalSystem system;

  @Nonnull
  private final Map<String, RentalVehicleType> vehicleTypes;

  public GbfsFreeVehicleStatusMapper(
    VehicleRentalSystem system,
    @Nullable Map<String, RentalVehicleType> vehicleTypes
  ) {
    this.system = system;
    this.vehicleTypes = new HashMap<>(requireNonNullElse(vehicleTypes, Map.of()));
  }

  public VehicleRentalVehicle mapFreeVehicleStatus(GBFSBike vehicle) {
    if (
      (vehicle.getStationId() == null || vehicle.getStationId().isBlank()) &&
      vehicle.getLon() != null &&
      vehicle.getLat() != null
    ) {
      VehicleRentalVehicle rentalVehicle = new VehicleRentalVehicle();
      rentalVehicle.id = new FeedScopedId(system.systemId, vehicle.getBikeId());
      rentalVehicle.system = system;
      rentalVehicle.name = new NonLocalizedString(getName(vehicle));
      rentalVehicle.longitude = vehicle.getLon();
      rentalVehicle.latitude = vehicle.getLat();
      rentalVehicle.vehicleType =
        vehicleTypes.getOrDefault(
          vehicle.getVehicleTypeId(),
          RentalVehicleType.getDefaultType(system.systemId)
        );
      rentalVehicle.isReserved = vehicle.getIsReserved() != null ? vehicle.getIsReserved() : false;
      rentalVehicle.isDisabled = vehicle.getIsDisabled() != null ? vehicle.getIsDisabled() : false;
      rentalVehicle.lastReported =
        vehicle.getLastReported() != null
          ? Instant.ofEpochSecond((long) (double) vehicle.getLastReported())
          : null;
      rentalVehicle.currentRangeMeters = vehicle.getCurrentRangeMeters();
      rentalVehicle.pricingPlanId = vehicle.getPricingPlanId();
      GBFSRentalUris rentalUris = vehicle.getRentalUris();
      if (rentalUris != null) {
        String androidUri = rentalUris.getAndroid();
        String iosUri = rentalUris.getIos();
        String webUri = rentalUris.getWeb();
        rentalVehicle.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
      }

      return rentalVehicle;
    } else {
      return null;
    }
  }

  private String getName(GBFSBike vehicle) {
    var typeId = vehicle.getVehicleTypeId();
    if (typeId != null) {
      var type = vehicleTypes.get(typeId);
      if (type != null && type.name != null) {
        return type.name;
      }
    }
    return RentalVehicleType.getDefaultType(system.systemId).name;
  }
}
