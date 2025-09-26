package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSName;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GbfsVehicleTypeMapper {

  private final String systemId;

  public GbfsVehicleTypeMapper(String systemId) {
    this.systemId = systemId;
  }

  public RentalVehicleType mapRentalVehicleType(GBFSVehicleType vehicleType) {
    return new RentalVehicleType(
      new FeedScopedId(systemId, vehicleType.getVehicleTypeId()),
      optionalLocalizedString(vehicleType.getName(), GBFSName::getLanguage, GBFSName::getText),
      fromGbfs(vehicleType.getFormFactor()),
      fromGbfs(vehicleType.getPropulsionType()),
      vehicleType.getMaxRangeMeters()
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
      case SCOOTER -> RentalFormFactor.SCOOTER;
      case SCOOTER_STANDING -> RentalFormFactor.SCOOTER_STANDING;
      case SCOOTER_SEATED -> RentalFormFactor.SCOOTER_SEATED;
      case OTHER -> RentalFormFactor.OTHER;
    };
  }
}
