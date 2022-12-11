package org.opentripplanner.ext.vehiclerentalservicedirectory;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public record GbfsDataSourceParameters(
  String url,
  String language,
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
