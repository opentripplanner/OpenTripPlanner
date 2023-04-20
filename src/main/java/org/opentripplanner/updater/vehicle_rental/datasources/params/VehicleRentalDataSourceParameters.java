package org.opentripplanner.updater.vehicle_rental.datasources.params;

import javax.annotation.Nonnull;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public interface VehicleRentalDataSourceParameters {
  @Nonnull
  String url();

  @Nonnull
  VehicleRentalSourceType sourceType();

  @Nonnull
  HttpHeaders httpHeaders();
}
