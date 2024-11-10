package org.opentripplanner.service.vehicleparking.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

/**
 * Service that holds all the {@link VehicleParking} instances and an index for fetching parking
 * locations within a {@link VehicleParkingGroup}. This class is thread-safe because the collections
 * held here are immutable and only updated in atomic operations that replace the existing
 * collection with a new copy.
 *
 * <P>THIS CLASS IS THREAD-SAFE<p>
 */
@Singleton
public class DefaultVehicleParkingService implements Serializable, VehicleParkingService {

  @Inject
  public DefaultVehicleParkingService() {}

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
  private volatile ImmutableListMultimap<VehicleParkingGroup, VehicleParking> vehicleParkingGroups = ImmutableListMultimap.of();

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

  @Override
  public Stream<VehicleParking> getBikeParks() {
    return vehicleParkings.stream().filter(VehicleParking::hasBicyclePlaces);
  }

  @Override
  public Stream<VehicleParking> getCarParks() {
    return vehicleParkings.stream().filter(VehicleParking::hasAnyCarPlaces);
  }

  @Override
  public Stream<VehicleParking> getVehicleParkings() {
    return vehicleParkings.stream();
  }

  @Override
  public ImmutableListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups() {
    return vehicleParkingGroups;
  }

  @Override
  public boolean hasBikeParking() {
    return vehicleParkings.stream().anyMatch(VehicleParking::hasBicyclePlaces);
  }

  @Override
  public boolean hasCarParking() {
    return vehicleParkings.stream().anyMatch(VehicleParking::hasAnyCarPlaces);
  }
}
