package org.opentripplanner.service.realtimevehicles;

import java.util.List;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A read-only, request-scoped view over the {@link RealtimeVehicleRepositorySnapshot} that resolves
 * patterns created by real-time updates.
 */
public interface RealtimeVehicleService {
  /**
   * Get the realtime vehicles for a certain trip pattern, either a scheduled pattern or a
   * pattern created by a real-time update. Service contains all the vehicles that exist in
   * input feeds but doesn't store any historical data.
   */
  List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern);

  /**
   * Get the latest occupancy status for a certain trip. Service contains all the vehicles that
   * exist in input feeds but doesn't store any historical data.
   */
  OccupancyStatus getVehicleOccupancyStatus(Trip trip);
}
