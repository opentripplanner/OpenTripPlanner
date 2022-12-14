package org.opentripplanner.ext.smoovebikerental;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

/**
 * @param allowOverloading Do the stations in the network allow overloading (ignoring available spaces)
 */
public record SmooveBikeRentalDataSourceParameters(
  String url,
  String network,
  boolean allowOverloading,
  @Nonnull Map<String, String> httpHeaders
)
  implements VehicleRentalDataSourceParameters {
  /**
   * Each updater can be assigned a unique network ID in the configuration to prevent returning
   * bikes at stations for another network.
   * TODO shouldn't we give each updater a unique network ID by default?
   */
  @Nullable
  public String getNetwork(String defaultValue) {
    return network == null || network.isEmpty() ? defaultValue : network;
  }

  public boolean isAllowOverloading() {
    return allowOverloading;
  }

  @Nonnull
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.SMOOVE;
  }
}
