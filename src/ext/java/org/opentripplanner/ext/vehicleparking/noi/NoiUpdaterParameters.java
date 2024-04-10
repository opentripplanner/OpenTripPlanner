package org.opentripplanner.ext.vehicleparking.noi;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * NoiUpdater}.
 */
public record NoiUpdaterParameters(
  String configRef,
  URI url,
  String feedId,
  Duration frequency,
  HttpHeaders httpHeaders
)
  implements VehicleParkingUpdaterParameters {
  @Override
  public VehicleParkingSourceType sourceType() {
    return VehicleParkingSourceType.NOI_OPEN_DATA_HUB;
  }
}
