package org.opentripplanner.service.vehiclepositions;

import java.util.List;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.network.TripPattern;

public interface VehiclePositionService {
  /**
   * Get the vehicle positions for a certain trip.
   */
  List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern);
}
