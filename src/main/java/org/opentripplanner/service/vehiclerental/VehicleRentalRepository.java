package org.opentripplanner.service.vehiclerental;

import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * The writable data store of vehicle rental information.
 */
public interface VehicleRentalRepository {
  void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation);

  void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId);
}
