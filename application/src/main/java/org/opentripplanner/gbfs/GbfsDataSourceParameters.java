package org.opentripplanner.gbfs;

import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;

/**
 * Parameters needed by the GBFS feed loader and mappers, defined as an interface in the gbfs
 * package to avoid a dependency from gbfs back to the updater/vehicle_rental package.
 */
public interface GbfsDataSourceParameters {
  String url();

  @Nullable
  String language();

  HttpHeaders httpHeaders();

  @Nullable
  String network();

  boolean allowKeepingRentedVehicleAtDestination();

  boolean geofencingZones();

  boolean overloadingAllowed();

  boolean allowStationRental();

  boolean allowFreeFloatingRental();
}
