package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.updater.AllowedRentalType;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public record GbfsVehicleRentalDataSourceParameters(
  String url,
  String language,
  boolean allowKeepingRentedVehicleAtDestination,
  HttpHeaders httpHeaders,
  String network,
  boolean geofencingZones,
  boolean overloadingAllowed,
  AllowedRentalType allowedRentalType
)
  implements VehicleRentalDataSourceParameters {
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.GBFS;
  }

  @Override
  public AllowedRentalType allowedRentalType() {
    return allowedRentalType;
  }
}
