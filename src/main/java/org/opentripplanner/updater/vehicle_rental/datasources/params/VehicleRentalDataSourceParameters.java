package org.opentripplanner.updater.vehicle_rental.datasources.params;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public interface VehicleRentalDataSourceParameters {
  @Nonnull
  String url();

  @Nullable
  String network();

  @Nonnull
  VehicleRentalSourceType sourceType();

  @Nonnull
  HttpHeaders httpHeaders();
}
