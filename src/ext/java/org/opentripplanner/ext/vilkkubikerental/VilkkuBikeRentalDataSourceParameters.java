package org.opentripplanner.ext.vilkkubikerental;

import javax.annotation.Nonnull;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public record VilkkuBikeRentalDataSourceParameters(
  String url,
  String network,
  boolean allowOverloading,
  HttpHeaders httpHeaders
)
  implements VehicleRentalDataSourceParameters {
  @Nonnull
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.VILKKU;
  }
}
