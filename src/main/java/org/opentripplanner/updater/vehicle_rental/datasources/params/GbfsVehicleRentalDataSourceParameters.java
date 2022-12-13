package org.opentripplanner.updater.vehicle_rental.datasources.params;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public record GbfsVehicleRentalDataSourceParameters(
  String url,
  String language,
  boolean allowKeepingRentedVehicleAtDestination,
  Map<String, String> httpHeaders,
  String network
)
  implements VehicleRentalDataSourceParameters {
  @Nonnull
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.GBFS;
  }
}
