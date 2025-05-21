package org.opentripplanner.ext.vehicleparking.sirifm;

import static org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType.SIRI_FM;
import static org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters.UpdateType.AVAILABILITY_ONLY;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * SiriFmDataSource}.
 */
public record SiriFmUpdaterParameters(
  String configRef,
  URI url,
  String feedId,
  Duration frequency,
  HttpHeaders httpHeaders
)
  implements VehicleParkingUpdaterParameters {
  @Override
  public VehicleParkingSourceType sourceType() {
    return SIRI_FM;
  }

  @Override
  public UpdateType updateType() {
    return AVAILABILITY_ONLY;
  }
}
