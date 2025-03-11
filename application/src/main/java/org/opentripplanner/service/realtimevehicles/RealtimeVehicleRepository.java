package org.opentripplanner.service.realtimevehicles;

import com.google.common.collect.Multimap;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

public interface RealtimeVehicleRepository {
  /**
   * Stores the relationship between of realtime vehicles with a pattern for a given feed id.
   * If the pattern is a realtime-added one, then the original (scheduled) one is used as the key
   * for the map storing the information.
   * <p>
   * Before storing the new vehicles, it removes the previous updates for the given {@code feedId}.
   */
  void setRealtimeVehicles(String feedId, Multimap<TripPattern, RealtimeVehicle> updates);

  /**
   * Get the vehicles for a certain trip.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);
}
