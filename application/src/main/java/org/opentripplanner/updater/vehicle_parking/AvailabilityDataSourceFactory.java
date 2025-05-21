package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.ext.vehicleparking.sirifm.SiriFmDataSource;
import org.opentripplanner.ext.vehicleparking.sirifm.SiriFmUpdaterParameters;
import org.opentripplanner.updater.spi.DataSource;

/**
 * Class that can be used to return a custom vehicle parking {@link DataSource}.
 */
public class AvailabilityDataSourceFactory {

  public static DataSource<AvailabiltyUpdate> create(VehicleParkingUpdaterParameters parameters) {
    return switch (parameters.sourceType()) {
      case SIRI_FM -> new SiriFmDataSource((SiriFmUpdaterParameters) parameters);
      case PARK_API, BICYCLE_PARK_API, LIIPI, BIKEEP, BIKELY -> throw new IllegalArgumentException(
        "Cannot instantiate SIRI-FM data source"
      );
    };
  }
}
