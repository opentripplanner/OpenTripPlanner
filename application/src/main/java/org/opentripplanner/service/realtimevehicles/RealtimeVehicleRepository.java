package org.opentripplanner.service.realtimevehicles;

import com.google.common.collect.Multimap;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Stores the realtime vehicles. There is one instance for the whole application: it is written
 * by the vehicle-position updater on the graph writer thread and read concurrently by request
 * threads, usually through the request-scoped {@link RealtimeVehicleService}.
 */
public interface RealtimeVehicleRepository {
  /**
   * Stores all realtime vehicles for a given {@code feedId} and associates each with a pattern.
   * If the pattern is a realtime-added one, then the original (scheduled) one is used as the key
   * for the map storing the information.
   * <p>
   * Before storing the new vehicles, it removes the previous updates for the given {@code feedId}.
   */
  void setRealtimeVehiclesForFeed(String feedId, Multimap<TripPattern, RealtimeVehicle> updates);

  /**
   * Get the vehicles stored for the given pattern key. This is a raw lookup: the pattern must be
   * the exact key used when storing. Use
   * {@link RealtimeVehicleService#getRealtimeVehicles(TripPattern)} to also resolve patterns
   * created by real-time updates.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);
}
