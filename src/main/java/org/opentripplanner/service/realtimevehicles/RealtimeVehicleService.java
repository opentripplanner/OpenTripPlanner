package org.opentripplanner.service.realtimevehicles;

import java.util.List;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;

public interface RealtimeVehicleService {
  /**
   * Get the realtime vehicles for a certain trip pattern.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);

  /**
   * Get the latest occupancy status for a certain trip.
   */
  OccupancyStatus getVehicleOccupancyStatus(TripPattern pattern, FeedScopedId tripId);
}
