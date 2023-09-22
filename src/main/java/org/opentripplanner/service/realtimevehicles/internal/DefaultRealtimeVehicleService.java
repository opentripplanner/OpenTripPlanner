package org.opentripplanner.service.realtimevehicles.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;

@Singleton
public class DefaultRealtimeVehicleService
  implements RealtimeVehicleService, RealtimeVehicleRepository {

  private final Map<TripPattern, List<RealtimeVehicle>> vehicles = new ConcurrentHashMap<>();

  @Inject
  public DefaultRealtimeVehicleService() {}

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
  public OccupancyStatus getVehicleOccupancyStatus(TripPattern pattern, FeedScopedId tripId) {
    return vehicles
      .getOrDefault(pattern, List.of())
      .stream()
      .filter(vehicle -> tripId.equals(vehicle.trip().getId()))
      .max(Comparator.comparing(vehicle -> vehicle.time()))
      .map(vehicle -> vehicle.occupancyStatus())
      .orElse(OccupancyStatus.NO_DATA_AVAILABLE);
  }
}
