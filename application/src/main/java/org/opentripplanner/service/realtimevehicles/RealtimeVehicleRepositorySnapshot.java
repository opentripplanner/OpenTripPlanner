package org.opentripplanner.service.realtimevehicles;

import java.util.List;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * An immutable, read-only snapshot of the realtime vehicles. A new snapshot is published each time
 * a transaction that touched the {@link RealtimeVehicleRepository} commits. Request threads read a
 * snapshot resolved at the start of the request, usually through the request-scoped
 * {@link RealtimeVehicleService}.
 */
public interface RealtimeVehicleRepositorySnapshot {
  /**
   * Get the vehicles stored for the given pattern key. This is a raw lookup: the pattern must be
   * the exact key used when storing. Use
   * {@link RealtimeVehicleService#getRealtimeVehicles(TripPattern)} to also resolve patterns
   * created by real-time updates.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);
}
