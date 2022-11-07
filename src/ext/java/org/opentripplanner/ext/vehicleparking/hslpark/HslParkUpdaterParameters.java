package org.opentripplanner.ext.vehicleparking.hslpark;

import java.time.ZoneId;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * HslParkUpdater}.
 */
public record HslParkUpdaterParameters(
  String configRef,
  int facilitiesFrequencySec,
  String facilitiesUrl,
  String feedId,
  DataSourceType sourceType,
  int utilizationsFrequencySec,
  String utilizationsUrl,
  ZoneId timeZone,
  String hubsUrl
)
  implements VehicleParkingUpdaterParameters {
  @Override
  public int frequencySec() {
    return utilizationsFrequencySec;
  }
}
