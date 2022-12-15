package org.opentripplanner.updater.vehicle_rental.datasources.params;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;

public interface VehicleRentalDataSourceParameters {
  @Nonnull
  String url();

  @Nonnull
  VehicleRentalSourceType sourceType();

  @Nonnull
  Map<String, String> httpHeaders();
}
