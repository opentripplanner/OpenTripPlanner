package org.opentripplanner.updater.vehicle_rental.datasources;

import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class GbfsVehicleTypeMapper {

  private final String systemId;

  public GbfsVehicleTypeMapper(String systemId) {
    this.systemId = systemId;
  }

  public RentalVehicleType mapRentalVehicleType(GBFSVehicleType vehicleType) {
    return new RentalVehicleType(
      new FeedScopedId(systemId, vehicleType.getVehicleTypeId()),
      NonLocalizedString.ofNullable(vehicleType.getName()),
      fromGbfs(vehicleType.getFormFactor()),
      RentalVehicleType.PropulsionType.fromGbfs(vehicleType.getPropulsionType()),
      vehicleType.getMaxRangeMeters()
    );
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
