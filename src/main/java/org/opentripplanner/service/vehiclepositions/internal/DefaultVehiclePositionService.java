package org.opentripplanner.service.vehiclepositions.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.service.vehiclepositions.VehiclePositionRepository;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.network.TripPattern;

@Singleton
public class DefaultVehiclePositionService
  implements VehiclePositionService, VehiclePositionRepository {

  private final Map<TripPattern, List<RealtimeVehiclePosition>> positions = new ConcurrentHashMap<>();

  @Inject
  public DefaultVehiclePositionService() {}

  @Override
  public void setVehiclePositions(TripPattern pattern, List<RealtimeVehiclePosition> updates) {
    positions.put(pattern, List.copyOf(updates));
  }

  @Override
  public void clearVehiclePositions(TripPattern pattern) {
    positions.remove(pattern);
  }

  @Override
  public List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern) {
    // the list is made immutable during insertion, so we can safely return them
    return positions.getOrDefault(pattern, List.of());
  }
}
