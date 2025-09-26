package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static java.util.Objects.requireNonNullElse;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v3_0.vehicle_status.GBFSVehicle;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.*;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Ratio;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.logging.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GbfsVehicleStatusMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsVehicleStatusMapper.class);
  private static final Throttle LOG_THROTTLE = Throttle.ofOneMinute();

  private final VehicleRentalSystem system;

  private final Map<String, RentalVehicleType> vehicleTypes;

  public GbfsVehicleStatusMapper(
    VehicleRentalSystem system,
    @Nullable Map<String, RentalVehicleType> vehicleTypes
  ) {
    this.system = system;
    this.vehicleTypes = new HashMap<>(requireNonNullElse(vehicleTypes, Map.of()));
  }

  public VehicleRentalVehicle mapVehicleStatus(GBFSVehicle vehicle) {
    if (
      (vehicle.getStationId() == null || vehicle.getStationId().isBlank()) &&
      vehicle.getLon() != null &&
      vehicle.getLat() != null
    ) {
      var fuelRatio = Ratio.ofBoxed(vehicle.getCurrentFuelPercent(), validationErrorMessage ->
        LOG_THROTTLE.throttle(() ->
          LOG.warn("'currentFuelPercent' is not valid. Details: {}", validationErrorMessage)
        )
      ).orElse(null);
      var rangeMeters = Distance.ofMetersBoxed(vehicle.getCurrentRangeMeters(), error -> {
        LOG_THROTTLE.throttle(() ->
          LOG.warn(
            "Current range meter value not valid: {} - {}",
            vehicle.getCurrentRangeMeters(),
            error
          )
        );
      }).orElse(null);
      // if the propulsion type has an engine current_range_meters is required
      if (
        vehicle.getVehicleTypeId() != null &&
        vehicleTypes.get(vehicle.getVehicleTypeId()) != null &&
        vehicleTypes.get(vehicle.getVehicleTypeId()).propulsionType() !=
        RentalVehicleType.PropulsionType.HUMAN &&
        rangeMeters == null
      ) {
        return null;
      }

      var builder = VehicleRentalVehicle.of()
        .withId(new FeedScopedId(system.systemId(), vehicle.getVehicleId()))
        .withSystem(system)
        .withName(getName(vehicle))
        .withLongitude(vehicle.getLon())
        .withLatitude(vehicle.getLat())
        .withVehicleType(
          vehicleTypes.getOrDefault(
            vehicle.getVehicleTypeId(),
            RentalVehicleType.getDefaultType(system.systemId())
          )
        )
        .withIsReserved(vehicle.getIsReserved() != null ? vehicle.getIsReserved() : false)
        .withIsDisabled(vehicle.getIsDisabled() != null ? vehicle.getIsDisabled() : false)
        .withFuel(RentalVehicleFuel.of().withPercent(fuelRatio).withRange(rangeMeters).build());

      return builder.build();
    } else {
      return null;
    }
  }

  private I18NString getName(GBFSVehicle vehicle) {
    var typeId = vehicle.getVehicleTypeId();
    if (typeId != null) {
      var type = vehicleTypes.get(typeId);
      if (type != null && type.name() != null) {
        return type.name();
      }
    }
    return RentalVehicleType.getDefaultType(system.systemId()).name();
  }
}
