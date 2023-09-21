package org.opentripplanner.service.vehiclepositions;

import java.util.List;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;

public interface VehiclePositionService {
  /**
   * Get the vehicle positions for a certain trip.
   */
  List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern);

  /**
   * Get the latest occupancy status for a certain trip.
   */
  OccupancyStatus getVehicleOccupancyStatus(TripPattern pattern, FeedScopedId tripId);
}
