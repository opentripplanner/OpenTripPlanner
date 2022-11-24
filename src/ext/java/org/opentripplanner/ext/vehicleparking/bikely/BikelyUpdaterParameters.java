package org.opentripplanner.ext.vehicleparking.bikely;

import java.util.Map;
import javax.annotation.Nonnull;
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
  @Nonnull Map<String, String> httpHeaders
)
  implements VehicleParkingUpdaterParameters {
  @Override
  public VehicleParkingSourceType sourceType() {
    return VehicleParkingSourceType.BIKELY;
  }
}
