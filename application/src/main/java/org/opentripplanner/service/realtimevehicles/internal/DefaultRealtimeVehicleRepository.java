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
  public DefaultRealtimeVehicleRepository(RealtimeVehicleRepositorySnapshot snapshot) {
    // the cast is safe: all snapshots are created by createSnapshot() below
    this.vehicles = ArrayListMultimap.create(((Snapshot) snapshot).vehicles);
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

  @Override
  public RealtimeVehicleRepositorySnapshot createSnapshot() {
    return new Snapshot(ImmutableListMultimap.copyOf(vehicles));
  }

  /** Immutable snapshot of the repository state, published at commit time. */
  private static class Snapshot implements RealtimeVehicleRepositorySnapshot {

    private final ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles;

    private Snapshot(ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles) {
      this.vehicles = vehicles;
    }

    @Override
    public List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern) {
      return vehicles.get(pattern);
    }
  }
}
