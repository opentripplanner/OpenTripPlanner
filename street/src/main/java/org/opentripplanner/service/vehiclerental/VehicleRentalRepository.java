package org.opentripplanner.service.vehiclerental;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingZoneIndex;

/**
 * The writable data store of vehicle rental information.
 */
public interface VehicleRentalRepository {
  void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation);

  void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId);

  /**
   * Register a geofencing zone index for a specific data source. Called by the vehicle rental
   * updater when zones are applied. Each updater (one per network) registers its own index;
   * re-registering with the same {@code dataSourceName} replaces the previous index.
   */
  void setGeofencingZoneIndex(String dataSourceName, GeofencingZoneIndex index);
}
