package org.opentripplanner.updater.vehicle_rental.datasources.params;

import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public interface VehicleRentalDataSourceParameters {
  String url();

  @Nullable
  String network();

  VehicleRentalSourceType sourceType();

  HttpHeaders httpHeaders();

  boolean allowRentalType(RentalPickupType rentalPickupType);
}
