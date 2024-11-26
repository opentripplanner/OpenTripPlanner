package org.opentripplanner.updater.vehicle_rental.datasources.params;

import java.util.Objects;
import java.util.Set;
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
  Set<RentalPickupType> rentalPickupTypes
)
  implements VehicleRentalDataSourceParameters {
  public GbfsVehicleRentalDataSourceParameters {
    Objects.requireNonNull(rentalPickupTypes);
  }
  @Override
  public VehicleRentalSourceType sourceType() {
    return VehicleRentalSourceType.GBFS;
  }

  @Override
  public boolean allowRentalType(RentalPickupType rentalPickupType) {
    return rentalPickupTypes.contains(rentalPickupType);
  }
}
