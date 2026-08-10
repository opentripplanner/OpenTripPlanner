package org.opentripplanner.gbfs.v3;

import static org.opentripplanner.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSName;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleType;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.utils.logging.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GbfsVehicleTypeMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsVehicleTypeMapper.class);
  private static final Throttle LOG_THROTTLE = Throttle.ofOneMinute();

  private final String systemId;

  public GbfsVehicleTypeMapper(String systemId) {
    this.systemId = systemId;
  }

  /**
   * Maps a GBFS vehicle type to the OTP model, or returns {@code null} when it cannot be mapped and
   * should be ignored.
   * <p>
   * {@code form_factor} and {@code propulsion_type} are required by the GBFS specification. When a
   * feed omits one, or sends a value the GBFS model does not recognize, it is deserialized to
   * {@code null}. Rather than guess a substitute we skip the vehicle type so that a single malformed
   * entry does not abort the whole feed update. Downstream consumers already treat an unknown vehicle
   * type id as either filtered out (stations) or the system default (free-floating vehicles).
   */
  @Nullable
  public RentalVehicleType mapRentalVehicleType(GBFSVehicleType vehicleType) {
    if (vehicleType.getFormFactor() == null) {
      logIgnoredVehicleType(vehicleType, "form_factor");
      return null;
    }
    if (vehicleType.getPropulsionType() == null) {
      logIgnoredVehicleType(vehicleType, "propulsion_type");
      return null;
    }
    return new RentalVehicleType(
      new FeedScopedId(systemId, vehicleType.getVehicleTypeId()),
      optionalLocalizedString(vehicleType.getName(), GBFSName::getLanguage, GBFSName::getText),
      fromGbfs(vehicleType.getFormFactor()),
      fromGbfs(vehicleType.getPropulsionType()),
      vehicleType.getMaxRangeMeters()
    );
  }

  private void logIgnoredVehicleType(GBFSVehicleType vehicleType, String field) {
    LOG_THROTTLE.throttle(() ->
      LOG.info(
        "Ignoring rental vehicle type '{}' in feed '{}': missing or unrecognized {}. {}",
        vehicleType.getVehicleTypeId(),
        systemId,
        field,
        LOG_THROTTLE.setupInfo()
      )
    );
  }

  public static RentalVehicleType.PropulsionType fromGbfs(
    GBFSVehicleType.PropulsionType propulsionType
  ) {
    return switch (propulsionType) {
      case HUMAN -> RentalVehicleType.PropulsionType.HUMAN;
      case ELECTRIC_ASSIST -> RentalVehicleType.PropulsionType.ELECTRIC_ASSIST;
      case ELECTRIC -> RentalVehicleType.PropulsionType.ELECTRIC;
      case COMBUSTION -> RentalVehicleType.PropulsionType.COMBUSTION;
      case COMBUSTION_DIESEL -> RentalVehicleType.PropulsionType.COMBUSTION_DIESEL;
      case HYBRID -> RentalVehicleType.PropulsionType.HYBRID;
      case PLUG_IN_HYBRID -> RentalVehicleType.PropulsionType.PLUG_IN_HYBRID;
      case HYDROGEN_FUEL_CELL -> RentalVehicleType.PropulsionType.HYDROGEN_FUEL_CELL;
    };
  }

  private static RentalFormFactor fromGbfs(GBFSVehicleType.FormFactor formFactor) {
    return switch (formFactor) {
      case BICYCLE -> RentalFormFactor.BICYCLE;
      case CARGO_BICYCLE -> RentalFormFactor.CARGO_BICYCLE;
      case CAR -> RentalFormFactor.CAR;
      case MOPED -> RentalFormFactor.MOPED;
      case SCOOTER_STANDING -> RentalFormFactor.SCOOTER_STANDING;
      case SCOOTER_SEATED -> RentalFormFactor.SCOOTER_SEATED;
      case OTHER -> RentalFormFactor.OTHER;
    };
  }
}
