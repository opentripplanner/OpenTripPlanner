package org.opentripplanner.service.vehiclepositions.internal;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.network.TripPattern;

@Singleton
public class DefaultVehiclePositionService implements VehiclePositionService {

  private final Map<TripPattern, List<RealtimeVehiclePosition>> positions = new ConcurrentHashMap<>();

  @Inject
  public DefaultVehiclePositionService() {}

  /**
   * For the given pattern set all realtime vehicle positions.
   * <p>
   * The list is expected to be exhaustive: all existing positions will be overridden.
   * <p>
   * This means that if there are two updaters providing positions for the same pattern they
   * overwrite each other.
   */
  @Override
  public void setVehiclePositions(TripPattern pattern, List<RealtimeVehiclePosition> updates) {
    positions.put(pattern, List.copyOf(updates));
  }

  /**
   * Remove all vehicle positions for a given pattern.
   * <p>
   * This is useful to clear old vehicles for which there are no more updates and we assume that
   * they have stopped their trip.
   */
  @Override
  public void clearVehiclePositions(TripPattern pattern) {
    positions.remove(pattern);
  }

  /**
   * Get the vehicle positions for a certain trip.
   */
  @Override
  public List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern) {
    // the list is made immutable during insertion, so we can safely return them
    return positions.getOrDefault(pattern, List.of());
  }
}
