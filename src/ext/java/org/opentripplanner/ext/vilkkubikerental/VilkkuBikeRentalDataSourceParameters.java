package org.opentripplanner.ext.vilkkubikerental;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public record VilkkuBikeRentalDataSourceParameters(
  String url,
  String network,
  boolean allowOverloading,
  @Nonnull Map<String, String> httpHeaders
)
  implements VehicleRentalDataSourceParameters {
  @Nonnull
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.VILKKU;
  }
}
