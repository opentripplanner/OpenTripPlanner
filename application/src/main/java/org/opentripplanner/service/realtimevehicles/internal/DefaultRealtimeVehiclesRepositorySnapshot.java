package org.opentripplanner.service.realtimevehicles.internal;

import com.google.common.collect.ImmutableListMultimap;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

/** Immutable snapshot of the repository state, published at commit time. */
class DefaultRealtimeVehiclesRepositorySnapshot implements RealtimeVehicleRepositorySnapshot {

  private final ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles;

  DefaultRealtimeVehiclesRepositorySnapshot(
    ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles
  ) {
    this.vehicles = vehicles;
  }

  @Override
  public List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern) {
    return vehicles.get(pattern);
  }

  /** used by the repository life-cycle */
  DefaultRealtimeVehicleRepository copyOnWrite() {
    return new DefaultRealtimeVehicleRepository(vehicles);
  }
}
