package org.opentripplanner.service.realtimevehicles.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Mutable repository for the realtime vehicles. A new instance is created for each transaction
 * that writes vehicles — initialized from the last committed snapshot — and is only accessed on
 * the single writer thread. {@link #createSnapshot()} publishes an immutable snapshot of its
 * state at commit time, safe for concurrent reads from request threads.
 */
public class DefaultRealtimeVehicleRepository implements RealtimeVehicleRepository {

  private final ListMultimap<TripPattern, RealtimeVehicle> vehicles;

  /** Create an empty repository. */
  public DefaultRealtimeVehicleRepository() {
    this.vehicles = ArrayListMultimap.create();
  }

  /** Create a repository initialized with the state of the given snapshot. */
  DefaultRealtimeVehicleRepository(ListMultimap<TripPattern, RealtimeVehicle> vehicles) {
    this.vehicles = ArrayListMultimap.create(vehicles);
  }

  @Override
  public void setRealtimeVehiclesForFeed(
    String feedId,
    Multimap<TripPattern, RealtimeVehicle> updates
  ) {
    // remove all previous updates for this specific feed id
    List.copyOf(vehicles.keySet())
      .stream()
      .filter(p -> p.getFeedId().equals(feedId))
      .forEach(vehicles::removeAll);
    // transform keys and put all fresh updates into map
    updates.forEach((pattern, vehicle) -> {
      if (pattern.getOriginalTripPattern() != null) {
        pattern = pattern.getOriginalTripPattern();
      }
      vehicles.put(pattern, vehicle);
    });
  }

  /**
   * Produce an immutable copy of the state to be used in the repository life-cycle. Only
   * the lifecycle should have access to this, hence the package local access.
   */
  RealtimeVehicleRepositorySnapshot freeze() {
    return new DefaultRealtimeVehiclesRepositorySnapshot(ImmutableListMultimap.copyOf(vehicles));
  }
}
