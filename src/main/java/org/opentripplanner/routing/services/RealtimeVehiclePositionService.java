package org.opentripplanner.routing.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.network.TripPattern;

public class RealtimeVehiclePositionService {

  private final Map<TripPattern, List<RealtimeVehiclePosition>> positions = new ConcurrentHashMap<>();

  /**
   * For the given pattern set all realtime vehicle positions.
   * <p>
   * The list is expected to be exhaustive: all existing positions will be overridden.
   * <p>
   * This means that if there are two updaters providing positions for the same pattern they
   * overwrite each other.
   */
  public void setVehiclePositions(TripPattern pattern, List<RealtimeVehiclePosition> updates) {
    positions.put(pattern, List.copyOf(updates));
  }

  /**
   * Remove all vehicle positions for a given pattern.
   * <p>
   * This is useful to clear old vehicles for which there are no more updates and we assume that
   * they have stopped their trip.
   */
  public void clearVehiclePositions(TripPattern pattern) {
    positions.remove(pattern);
  }

  /**
   * Get the vehicle positions for a certain trip.
   */
  public List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern) {
    // the list is made immutable during insertion, so we can safely return them
    return positions.getOrDefault(pattern, List.of());
  }
}
