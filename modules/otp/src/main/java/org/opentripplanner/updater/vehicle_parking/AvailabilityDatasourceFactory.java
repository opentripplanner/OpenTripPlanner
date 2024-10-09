package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.ext.vehicleparking.sirifm.SiriFmDatasource;
import org.opentripplanner.ext.vehicleparking.sirifm.SiriFmUpdaterParameters;
import org.opentripplanner.updater.spi.DataSource;

/**
 * Class that can be used to return a custom vehicle parking {@link DataSource}.
 */
public class AvailabilityDatasourceFactory {

  public static DataSource<AvailabiltyUpdate> create(VehicleParkingUpdaterParameters parameters) {
    return switch (parameters.sourceType()) {
      case SIRI_FM -> new SiriFmDatasource((SiriFmUpdaterParameters) parameters);
      case PARK_API,
        BICYCLE_PARK_API,
        HSL_PARK,
        BIKEEP,
        BIKELY -> throw new IllegalArgumentException("Cannot instantiate SIRI-FM data source");
    };
  }
}
