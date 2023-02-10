package org.opentripplanner.service.vehiclerental;

import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public interface VehicleRentalRepository {
  void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation);

  void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId);

}
