package org.opentripplanner.ext.vilkkubikerental;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VilkkuBikeRentalDataSourceParameters extends VehicleRentalDataSourceParameters {

  private final boolean allowOverloading;
  private final String network;

  public VilkkuBikeRentalDataSourceParameters(
    String url,
    String network,
    boolean allowOverloading,
    @Nonnull Map<String, String> httpHeaders
  ) {
    super(VehicleRentalSourceType.VILKKU, url, httpHeaders);
    this.network = network;
    this.allowOverloading = allowOverloading;
  }

  public String network() {
    return network;
  }
}
