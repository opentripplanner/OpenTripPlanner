package org.opentripplanner.updater.vehicle_rental.datasources;

import java.time.Instant;
import java.util.Map;
import org.entur.gbfs.v2_2.free_bike_status.GBFSBike;
import org.entur.gbfs.v2_2.free_bike_status.GBFSRentalUris;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class GbfsFreeVehicleStatusMapper {

  private final VehicleRentalSystem system;
  private final Map<String, RentalVehicleType> vehicleTypes;

  public GbfsFreeVehicleStatusMapper(
    VehicleRentalSystem system,
    Map<String, RentalVehicleType> vehicleTypes
  ) {
    this.system = system;
    this.vehicleTypes = vehicleTypes;
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
      rentalVehicle.name = new NonLocalizedString(vehicle.getBikeId());
      rentalVehicle.longitude = vehicle.getLon();
      rentalVehicle.latitude = vehicle.getLat();
      rentalVehicle.vehicleType =
        vehicleTypes == null
          ? RentalVehicleType.getDefaultType(system.systemId)
          : vehicleTypes.get(vehicle.getVehicleTypeId());
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
}
