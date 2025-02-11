package org.opentripplanner.ext.smoovebikerental;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

/**
 * @param overloadingAllowed Do the stations in the network allow overloading (ignoring available spaces)
 */
public record SmooveBikeRentalDataSourceParameters(
  String url,
  String network,
  boolean overloadingAllowed,
  HttpHeaders httpHeaders,
  Set<RentalPickupType> rentalPickupTypes
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

  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.SMOOVE;
  }

  @Override
  public boolean allowRentalType(RentalPickupType rentalPickupType) {
    return rentalPickupTypes.contains(rentalPickupType);
  }
}
