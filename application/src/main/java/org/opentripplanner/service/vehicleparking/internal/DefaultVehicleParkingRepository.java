package org.opentripplanner.service.vehicleparking.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

@Singleton
public class DefaultVehicleParkingRepository implements VehicleParkingRepository {

  @Inject
  public DefaultVehicleParkingRepository() {}

  /**
   * To ensure that his is thread-safe, the set stored here should always be immutable.
   * <p>
   * The volatile keyword is used to ensure safe publication by clearing CPU caches.
   */
  private volatile Set<VehicleParking> vehicleParkings = Set.of();

  /**
   * To ensure that his is thread-safe, {@link ImmutableListMultimap} is used.
   * <p>
   * The volatile keyword is used to ensure safe publication by clearing CPU caches.
   */
  private volatile ImmutableListMultimap<VehicleParkingGroup, VehicleParking> vehicleParkingGroups =
    ImmutableListMultimap.of();

  /**
   * Does atomic update of {@link VehicleParking} and index of {@link VehicleParkingGroup} in this
   * service by replacing the existing with a new copy that includes old ones that were not removed
   * in the update and the new ones that were added in the update.
   */
  @Override
  public void updateVehicleParking(
    Collection<VehicleParking> parkingToAdd,
    Collection<VehicleParking> parkingToRemove
  ) {
    Multimap<VehicleParkingGroup, VehicleParking> updatedVehicleParkingGroups =
      ArrayListMultimap.create(vehicleParkingGroups);
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

  @Override
  public Collection<VehicleParking> listVehicleParkings() {
    return Set.copyOf(vehicleParkings);
  }

  @Override
  public ListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups() {
    return vehicleParkingGroups;
  }
}
