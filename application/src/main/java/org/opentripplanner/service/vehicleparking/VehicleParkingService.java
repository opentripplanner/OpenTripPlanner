package org.opentripplanner.service.vehicleparking;

import com.google.common.collect.ImmutableListMultimap;
import java.util.Collection;
import java.util.stream.Stream;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

public interface VehicleParkingService {
  void updateVehicleParking(
    Collection<VehicleParking> parkingToAdd,
    Collection<VehicleParking> parkingToRemove
  );

  Stream<VehicleParking> getBikeParks();

  Stream<VehicleParking> getCarParks();

  Stream<VehicleParking> getVehicleParkings();

  ImmutableListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups();

  boolean hasBikeParking();

  boolean hasCarParking();
}
