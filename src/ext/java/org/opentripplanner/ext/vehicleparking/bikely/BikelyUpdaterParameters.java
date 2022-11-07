package org.opentripplanner.ext.vehicleparking.bikely;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by
 * {@link BikelyUpdater}.
 */
public record BikelyUpdaterParameters(
  String configRef,
  String url,
  String feedId,
  Duration frequency,
  @Nonnull Map<String, String> httpHeaders,
  ZoneId timeZone
)
  implements VehicleParkingUpdaterParameters {
  @Override
  public int frequencySec() {
    return (int) frequency.toSeconds();
  }

  @Override
  public DataSourceType sourceType() {
    return DataSourceType.BIKELY;
  }
}
