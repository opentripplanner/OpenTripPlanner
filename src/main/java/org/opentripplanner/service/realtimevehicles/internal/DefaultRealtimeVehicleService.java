package org.opentripplanner.service.realtimevehicles.internal;

import static org.opentripplanner.transit.model.timetable.OccupancyStatus.NO_DATA_AVAILABLE;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

@Singleton
public class DefaultRealtimeVehicleService
  implements RealtimeVehicleService, RealtimeVehicleRepository {

  private final Map<TripPattern, List<RealtimeVehicle>> vehicles = new ConcurrentHashMap<>();

  private final TransitService transitService;

  @Inject
  public DefaultRealtimeVehicleService(TransitService transitService) {
    this.transitService = transitService;
  }

  @Override
  public void setRealtimeVehicles(TripPattern pattern, List<RealtimeVehicle> updates) {
    vehicles.put(pattern, List.copyOf(updates));
  }

  @Override
  public void clearRealtimeVehicles(TripPattern pattern) {
    vehicles.remove(pattern);
  }

  @Override
  public List<RealtimeVehicle> getRealtimeVehicles(@Nonnull TripPattern pattern) {
    // the list is made immutable during insertion, so we can safely return them
    return vehicles.getOrDefault(pattern, List.of());
  }

  @Nonnull
  @Override
  public OccupancyStatus getVehicleOccupancyStatus(@Nonnull Trip trip) {
    return getOccupancyStatus(trip.getId(), transitService.getPatternForTrip(trip));
  }

  /**
   * Get the latest occupancy status for a certain trip. Service contains all the vehicles that
   * exist in input feeds but doesn't store any historical data. This method is an alternative to
   * {@link #getVehicleOccupancyStatus(Trip)} and works even when {@link TransitService} is not
   * provided to the service.
   */
  public OccupancyStatus getOccupancyStatus(FeedScopedId tripId, TripPattern pattern) {
    return vehicles
      .getOrDefault(pattern, List.of())
      .stream()
      .filter(vehicle -> tripId.equals(vehicle.trip().getId()))
      .max(Comparator.comparing(vehicle -> vehicle.time().orElse(Instant.MIN)))
      .flatMap(RealtimeVehicle::occupancyStatus)
      .orElse(NO_DATA_AVAILABLE);
  }
}
