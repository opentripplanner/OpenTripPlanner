package org.opentripplanner.ext.vehiclerentalgeofencing.parameters;

import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.gbfs.GbfsDataSourceParameters;

/**
 * Parameters for loading one network's GBFS feed during graph build, constructed per dataset
 * discovered in the manifest so it can be handed to
 * {@link org.opentripplanner.gbfs.GbfsFeedLoaderAndMapper}.
 * <p>
 * Only geofencing zones are loaded here: vehicles and stations are realtime data supplied
 * by the vehicle rental updater, so the rental-related flags are all off.
 */
public record VehicleRentalNetworkDataSourceParameters(
  String url,
  @Nullable String network,
  @Nullable String language,
  HttpHeaders httpHeaders
) implements GbfsDataSourceParameters {
  public VehicleRentalNetworkDataSourceParameters {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("GBFS feed URL is required");
    }
    if (httpHeaders == null) {
      httpHeaders = HttpHeaders.empty();
    }
  }

  @Override
  public boolean allowKeepingRentedVehicleAtDestination() {
    return false;
  }

  @Override
  public boolean geofencingZones() {
    return true;
  }

  @Override
  public boolean overloadingAllowed() {
    return false;
  }

  @Override
  public boolean allowStationRental() {
    return true;
  }

  @Override
  public boolean allowFreeFloatingRental() {
    return true;
  }
}
