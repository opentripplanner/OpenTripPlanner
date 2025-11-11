package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.support;

import java.util.Map;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to filter out unknown vehicle types from GBFS data.
 */
public class UnknownVehicleTypeFilter {

  private static final Logger LOG = LoggerFactory.getLogger(UnknownVehicleTypeFilter.class);

  private final Map<String, RentalVehicleType> vehicleTypes;

  public UnknownVehicleTypeFilter(Map<String, RentalVehicleType> vehicleTypes) {
    this.vehicleTypes = vehicleTypes;
  }

  /**
   * Filter to check if a vehicle type exists in the vehicle types map.
   * Logs a debug message if the vehicle type is unknown.
   *
   * @param vehicleTypeId the vehicle type ID to check
   * @param stationId the station ID (for logging)
   * @param field the field name (for logging)
   * @return true if the vehicle type exists, false otherwise
   */
  public boolean filterUnknownVehicleType(String vehicleTypeId, String stationId, String field) {
    if (!vehicleTypes.containsKey(vehicleTypeId)) {
      LOG.debug(
        "Station {} references unknown vehicle type '{}' in {}, skipping",
        stationId,
        vehicleTypeId,
        field
      );
      return false;
    }
    return true;
  }
}
