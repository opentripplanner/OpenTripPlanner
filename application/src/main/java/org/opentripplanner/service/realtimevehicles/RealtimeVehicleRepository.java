package org.opentripplanner.service.realtimevehicles;

import com.google.common.collect.Multimap;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * The mutable repository for the realtime vehicles.
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
}
