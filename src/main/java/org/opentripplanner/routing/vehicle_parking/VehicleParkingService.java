package org.opentripplanner.routing.vehicle_parking;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Service that holds all the {@link VehicleParking} instances and an index for fetching parking
 * locations within a {@link VehicleParkingGroup}. This class is thread-safe because the collections
 * held here are immutable and only updated in atomic operations that replace the existing
 * collection with a new copy.
 *
 * <P>THIS CLASS IS THREAD-SAFE<p>
 */
public class VehicleParkingService implements Serializable {

  /**
   * To ensure that his is thread-safe, the set stored here should always be immutable.
   */
  private Set<VehicleParking> vehicleParkings = Set.of();

  /**
   * To ensure that his is thread-safe, {@link ImmutableListMultimap} is used.
   */
  private ImmutableListMultimap<VehicleParkingGroup, VehicleParking> vehicleParkingGroups = ImmutableListMultimap.of();

  /**
   * Does atomic update of {@link VehicleParking} and index of {@link VehicleParkingGroup} in this
   * service by replacing the existing with a new copy that includes old ones that were not removed
   * in the update and the new ones that were added in the update.
   */
  public void updateVehicleParking(
    Collection<VehicleParking> parkingToAdd,
    Collection<VehicleParking> parkingToRemove
  ) {
    Multimap<VehicleParkingGroup, VehicleParking> updatedVehicleParkingGroups = ArrayListMultimap.create(
      vehicleParkingGroups
    );
    parkingToRemove.forEach(vehicleParking -> {
      var vehicleParkingGroup = vehicleParking.getVehicleParkingGroup();
      if (vehicleParkingGroup != null) {
        updatedVehicleParkingGroups.remove(vehicleParking.getVehicleParkingGroup(), vehicleParking);
      }
    });
    parkingToAdd.forEach(vehicleParking -> {
      var vehicleParkingGroup = vehicleParking.getVehicleParkingGroup();
      if (vehicleParkingGroup != null) {
        updatedVehicleParkingGroups.put(vehicleParking.getVehicleParkingGroup(), vehicleParking);
      }
    });
    vehicleParkingGroups = ImmutableListMultimap.copyOf(updatedVehicleParkingGroups);

    Set<VehicleParking> updatedVehicleParkings = new HashSet<>(vehicleParkings);
    updatedVehicleParkings.removeAll(parkingToRemove);
    updatedVehicleParkings.addAll(parkingToAdd);
    vehicleParkings = Set.copyOf(updatedVehicleParkings);
  }

  public Stream<VehicleParking> getBikeParks() {
    return vehicleParkings.stream().filter(VehicleParking::hasBicyclePlaces);
  }

  public Stream<VehicleParking> getCarParks() {
    return vehicleParkings.stream().filter(VehicleParking::hasAnyCarPlaces);
  }

  public Stream<VehicleParking> getVehicleParkings() {
    return vehicleParkings.stream();
  }

  public ImmutableListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups() {
    return vehicleParkingGroups;
  }

  public boolean hasBikeParking() {
    return vehicleParkings.stream().anyMatch(VehicleParking::hasBicyclePlaces);
  }

  public boolean hasCarParking() {
    return vehicleParkings.stream().anyMatch(VehicleParking::hasAnyCarPlaces);
  }
}
