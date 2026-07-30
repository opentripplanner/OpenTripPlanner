package org.opentripplanner.service.realtimevehicles.internal;

import com.google.common.collect.ImmutableListMultimap;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Immutable snapshot of the realtime-vehicle repository state, published at commit time and safe
 * for concurrent reads from request threads. {@link #copyOnWrite()} starts the next transaction
 * from this state; both transitions are driven by the {@link RealtimeVehicleRepositoryLifecycle}.
 */
public class DefaultRealtimeVehicleRepositorySnapshot implements RealtimeVehicleRepositorySnapshot {

  private final ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles;

  DefaultRealtimeVehicleRepositorySnapshot(
    ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles
  ) {
    this.vehicles = vehicles;
  }

  @Override
  public List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern) {
    return vehicles.get(pattern);
  }

  /** Create a new mutable repository initialized with the state of this snapshot. */
  DefaultRealtimeVehicleRepository copyOnWrite() {
    return new DefaultRealtimeVehicleRepository(vehicles);
  }
}
