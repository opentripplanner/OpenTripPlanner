package org.opentripplanner.service.realtimevehicles;

import com.google.common.collect.Multimap;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;

public interface RealtimeVehicleRepository {
  /**
   * For the given pattern set all realtime vehicles.
   * <p>
   * The list is expected to be exhaustive: all existing vehicles will be overridden.
   * <p>
   * This means that if there are two updaters providing vehicles for the same pattern they
   * overwrite each other.
   */
  void setRealtimeVehicles(String feedId, Multimap<TripPattern, RealtimeVehicle> updates);

  /**
   * Get the vehicles for a certain trip.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);
}
