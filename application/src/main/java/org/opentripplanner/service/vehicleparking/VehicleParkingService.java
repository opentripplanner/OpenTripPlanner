package org.opentripplanner.service.vehicleparking;

import com.google.common.collect.ListMultimap;
import java.util.Collection;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

/**
 * The read-only service for getting information about parking facilities.
 * <p>
 * For writing data see {@link VehicleParkingRepository}
 */
public interface VehicleParkingService {
  Collection<VehicleParking> listBikeParks();

  Collection<VehicleParking> listCarParks();

  Collection<VehicleParking> listVehicleParkings();

  ListMultimap<VehicleParkingGroup, VehicleParking> listVehicleParkingGroups();

  boolean hasBikeParking();

  boolean hasCarParking();
}
