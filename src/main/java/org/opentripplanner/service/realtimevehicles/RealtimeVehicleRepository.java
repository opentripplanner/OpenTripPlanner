package org.opentripplanner.service.realtimevehicles;

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
  void setRealtimeVehicles(TripPattern pattern, List<RealtimeVehicle> updates);

  /**
   * Remove all vehicles for a given pattern.
   * <p>
   * This is useful to clear old vehicles for which there are no more updates and we assume that
   * they have stopped their trip.
   */
  void clearRealtimeVehicles(TripPattern pattern);
  /**
   * Get the vehicles for a certain trip.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);
}
