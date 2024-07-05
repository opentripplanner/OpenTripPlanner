package org.opentripplanner.ext.vehiclerentalservicedirectory.api;

import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

/**
 * Parameters for a specific network.
 * <p>
 * The {@link GbfsVehicleRentalDataSourceParameters} support  {@code overloadingAllowed} and
 * {@code allowKeepingRentedVehicleAtDestination} is not included here since they are not part of
 * the GBFS specification. If there is a demand for these, they can be added.
 * <p>
 * @param network The network name
 * @param geofencingZones enable geofencingZones for the given network
 */
public record NetworkParameters(String network, boolean geofencingZones) {
  public NetworkParameters withName(String network) {
    return new NetworkParameters(network, geofencingZones);
  }
}
