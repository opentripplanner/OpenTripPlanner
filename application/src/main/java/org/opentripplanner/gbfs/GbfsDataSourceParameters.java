package org.opentripplanner.gbfs;

import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;

/**
 * Parameters needed by the GBFS feed loader and mappers
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
