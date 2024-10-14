package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public record GbfsVehicleRentalDataSourceParameters(
  String url,
  String language,
  boolean allowKeepingRentedVehicleAtDestination,
  HttpHeaders httpHeaders,
  String network,
  boolean geofencingZones,
  boolean overloadingAllowed
)
  implements VehicleRentalDataSourceParameters {
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.GBFS;
  }
}
