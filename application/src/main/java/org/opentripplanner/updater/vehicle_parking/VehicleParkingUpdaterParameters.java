package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

/**
 * Class that implements {@link PollingGraphUpdaterParameters} and can be extended to include other
 * parameters required by a custom vehicle parking updater.
 */
public interface VehicleParkingUpdaterParameters extends PollingGraphUpdaterParameters {
  VehicleParkingSourceType sourceType();
  UpdateType updateType();

  enum UpdateType {
    FULL,
    AVAILABILITY_ONLY,
  }
}
