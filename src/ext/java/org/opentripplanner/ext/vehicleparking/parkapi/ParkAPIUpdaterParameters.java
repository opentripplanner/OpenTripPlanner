package org.opentripplanner.ext.vehicleparking.parkapi;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * ParkAPIUpdater}.
 */
public record ParkAPIUpdaterParameters(
  String configRef,
  String url,
  String feedId,
  int frequencySec,
  @Nonnull Map<String, String> httpHeaders,
  List<String> tags,
  DataSourceType sourceType,
  ZoneId timeZone
)
  implements VehicleParkingUpdaterParameters {}
