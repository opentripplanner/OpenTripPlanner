package org.opentripplanner.ext.vehicleparking.bikely;

import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by
 * {@link BikelyUpdater}.
 */
public record BikelyUpdaterParameters(
  String configRef,
  String url,
  String feedId,
  int frequencySec,
  HttpHeaders httpHeaders
)
  implements VehicleParkingUpdaterParameters {
  @Override
  public VehicleParkingSourceType sourceType() {
    return VehicleParkingSourceType.BIKELY;
  }
}
