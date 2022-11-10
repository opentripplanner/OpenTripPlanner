package org.opentripplanner.ext.vehicleparking.kml;

import org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * KmlBikeParkDataSource}.
 */
public record KmlUpdaterParameters(
  String configRef,
  String url,
  String feedId,
  String namePrefix,
  int frequencySec,
  boolean zip,
  VehicleParkingSourceType sourceType
)
  implements VehicleParkingUpdaterParameters {}
