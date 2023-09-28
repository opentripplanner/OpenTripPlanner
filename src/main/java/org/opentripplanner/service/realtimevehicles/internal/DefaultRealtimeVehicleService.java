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
  public List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern) {
    // the list is made immutable during insertion, so we can safely return them
    return vehicles.getOrDefault(pattern, List.of());
  }

  @Nonnull
  @Override
  public OccupancyStatus getVehicleOccupancyStatus(Trip trip) {
    TripPattern pattern = transitService.getPatternForTrip(trip);
    return vehicles
      .getOrDefault(pattern, List.of())
      .stream()
      .filter(vehicle -> trip.getId().equals(vehicle.trip().getId()))
      .max(Comparator.comparing(vehicle -> vehicle.time().orElse(Instant.MIN)))
      .map(vehicle -> vehicle.occupancyStatus())
      .orElse(Optional.empty())
      .orElse(NO_DATA_AVAILABLE);
  }
}
