package org.opentripplanner.service.vehicleparking;

import com.google.common.collect.ListMultimap;
import java.util.Collection;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

/**
 * The writable data store of parking facilities.
 */
public interface VehicleParkingRepository {
  void updateVehicleParking(
    Collection<VehicleParking> parkingToAdd,
    Collection<VehicleParking> parkingToRemove
  );
  Collection<VehicleParking> listVehicleParkings();

  ListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups();
}
