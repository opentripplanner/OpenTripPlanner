package org.opentripplanner.ext.vehiclerentalservicedirectory.api;

import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

/**
 * Parameters for a specific network.
 * <p>
 * The {@link GbfsVehicleRentalDataSourceParameters} supports {@code overloadingAllowed}
 * which is not included here since it is not part of
 * the GBFS specification. If there is a demand for it, it can be added.
 * <p>
 * @param network The network name
 * @param geofencingZones enable geofencingZones for the given network
 * @param allowKeepingAtDestination if a vehicle that was picked up from a station must be returned
 *                                  to another one or can be kept at the destination.
 *                                  {@link org.opentripplanner.standalone.config.routerconfig.updaters.sources.VehicleRentalSourceFactory#allOwKeepingRentedVehicleAtDestination()}
 */
public record NetworkParameters(
  String network,
  boolean geofencingZones,
  boolean allowKeepingAtDestination
) {}
