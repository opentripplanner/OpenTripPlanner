package org.opentripplanner.service.vehicleparking.internal;

import com.google.common.collect.ImmutableListMultimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.stream.Stream;
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
  public Stream<VehicleParking> getBikeParks() {
    return repository.getVehicleParkings().filter(VehicleParking::hasBicyclePlaces);
  }

  @Override
  public Stream<VehicleParking> getCarParks() {
    return repository.getVehicleParkings().filter(VehicleParking::hasAnyCarPlaces);
  }

  @Override
  public Stream<VehicleParking> getVehicleParkings() {
    return repository.getVehicleParkings();
  }

  @Override
  public ImmutableListMultimap<VehicleParkingGroup, VehicleParking> getVehicleParkingGroups() {
    return repository.getVehicleParkingGroups();
  }

  @Override
  public boolean hasBikeParking() {
    return repository.getVehicleParkings().anyMatch(VehicleParking::hasBicyclePlaces);
  }

  @Override
  public boolean hasCarParking() {
    return repository.getVehicleParkings().anyMatch(VehicleParking::hasAnyCarPlaces);
  }
}
