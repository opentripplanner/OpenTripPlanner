package org.opentripplanner.updater.vehicle_rental.datasources;

import static java.util.Objects.requireNonNullElse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSBike;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSRentalUris;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleFuel;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Ratio;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.logging.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsFreeVehicleStatusMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFreeVehicleStatusMapper.class);
  private static final Throttle LOG_THROTTLE = Throttle.ofOneMinute();

  private final VehicleRentalSystem system;

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
        .withId(new FeedScopedId(system.systemId(), vehicle.getBikeId()))
        .withSystem(system)
        .withName(new NonLocalizedString(getName(vehicle)))
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
        .withLastReported(
          vehicle.getLastReported() != null
            ? Instant.ofEpochSecond((long) (double) vehicle.getLastReported())
            : null
        )
        .withFuel(RentalVehicleFuel.of().withPercent(fuelRatio).withRange(rangeMeters).build())
        .withPricingPlanId(vehicle.getPricingPlanId());

      String availableUntil = vehicle.getAvailableUntil();
      if (StringUtils.hasValue(availableUntil)) {
        builder.withAvailableUntil(OffsetDateTime.parse(availableUntil));
      }

      GBFSRentalUris rentalUris = vehicle.getRentalUris();
      if (rentalUris != null) {
        String androidUri = rentalUris.getAndroid();
        String iosUri = rentalUris.getIos();
        String webUri = rentalUris.getWeb();
        builder.withRentalUris(
          VehicleRentalStationUris.of()
            .withAndroid(androidUri)
            .withIos(iosUri)
            .withWeb(webUri)
            .build()
        );
      }

      return builder.build();
    } else {
      return null;
    }
  }

  private String getName(GBFSBike vehicle) {
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
