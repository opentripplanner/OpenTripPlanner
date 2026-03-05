package org.opentripplanner.service.vehicleparking.internal;

import com.google.common.collect.ListMultimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.Collection;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
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

  private final VehicleParkingRepository repository;

  @Inject
  public DefaultVehicleParkingService(VehicleParkingRepository repository) {
    this.repository = repository;
  }

  @Override
  public Collection<VehicleParking> listBikeParks() {
    return repository
      .listVehicleParkings()
      .stream()
      .filter(VehicleParking::hasBicyclePlaces)
      .toList();
  }

  @Override
  public Collection<VehicleParking> listCarParks() {
    return repository
      .listVehicleParkings()
      .stream()
      .filter(VehicleParking::hasAnyCarPlaces)
      .toList();
  }

  @Override
  public Collection<VehicleParking> listVehicleParkings() {
    return repository.listVehicleParkings();
  }

  @Override
  public ListMultimap<VehicleParkingGroup, VehicleParking> listVehicleParkingGroups() {
    return repository.getVehicleParkingGroups();
  }

  @Override
  public boolean hasBikeParking() {
    return repository.listVehicleParkings().stream().anyMatch(VehicleParking::hasBicyclePlaces);
  }

  @Override
  public boolean hasCarParking() {
    return repository.listVehicleParkings().stream().anyMatch(VehicleParking::hasAnyCarPlaces);
  }
}
