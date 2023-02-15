package org.opentripplanner.service.vehiclepositions;

import java.util.List;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.network.TripPattern;

public interface VehiclePositionRepository {
  /**
   * For the given pattern set all realtime vehicle positions.
   * <p>
   * The list is expected to be exhaustive: all existing positions will be overridden.
   * <p>
   * This means that if there are two updaters providing positions for the same pattern they
   * overwrite each other.
   */
  void setVehiclePositions(TripPattern pattern, List<RealtimeVehiclePosition> updates);

  /**
   * Remove all vehicle positions for a given pattern.
   * <p>
   * This is useful to clear old vehicles for which there are no more updates and we assume that
   * they have stopped their trip.
   */
  void clearVehiclePositions(TripPattern pattern);
  /**
   * Get the vehicle positions for a certain trip.
   */
  List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern);
}
