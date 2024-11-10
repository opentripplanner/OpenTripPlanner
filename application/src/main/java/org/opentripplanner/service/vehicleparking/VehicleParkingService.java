package org.opentripplanner.service.vehicleparking;

import com.google.common.collect.ImmutableListMultimap;
import java.util.stream.Stream;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

/**
 * The read-only service for getting information about parking facilities.
 * <p>
 * For writing data see {@link VehicleParkingRepository}
 */
public interface VehicleParkingService {
  Stream<VehicleParking> getBikeParks();

  Stream<VehicleParking> getCarParks();

  Stream<VehicleParking> getVehicleParkings();

  ImmutableListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups();

  boolean hasBikeParking();

  boolean hasCarParking();
}
