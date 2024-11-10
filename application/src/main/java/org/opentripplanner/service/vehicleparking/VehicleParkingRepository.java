package org.opentripplanner.service.vehicleparking;

import java.util.Collection;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;

/**
 * The writable data store of parking facilities.
 */
public interface VehicleParkingRepository {
  void updateVehicleParking(
    Collection<VehicleParking> parkingToAdd,
    Collection<VehicleParking> parkingToRemove
  );
}
