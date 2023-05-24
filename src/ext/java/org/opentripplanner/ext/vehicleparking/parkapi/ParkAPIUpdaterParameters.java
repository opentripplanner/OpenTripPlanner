package org.opentripplanner.ext.vehicleparking.parkapi;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * ParkAPIUpdater}.
 */
public record ParkAPIUpdaterParameters(
  String configRef,
  String url,
  String feedId,
  Duration frequency,
  HttpHeaders httpHeaders,
  List<String> tags,
  VehicleParkingSourceType sourceType,
  ZoneId timeZone
)
  implements VehicleParkingUpdaterParameters {}
