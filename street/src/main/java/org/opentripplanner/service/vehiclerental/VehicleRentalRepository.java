package org.opentripplanner.service.vehiclerental;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

/**
 * The writable data store of vehicle rental information.
 */
public interface VehicleRentalRepository {
  void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation);

  void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId);
}
