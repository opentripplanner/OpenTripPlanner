package org.opentripplanner.service.realtimevehicles.internal;

import static org.opentripplanner.transit.model.timetable.OccupancyStatus.NO_DATA_AVAILABLE;

import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

/**
 * A request-scoped view over the {@link RealtimeVehicleRepository}.
 * <p>
 * A new instance should be created for each request, with the request's {@link TransitService},
 * so that the whole request sees one consistent timetable snapshot.
 */
public class DefaultRealtimeVehicleService implements RealtimeVehicleService {

  private final RealtimeVehicleRepository repository;
  private final TransitService transitService;

  @Inject
  public DefaultRealtimeVehicleService(
    RealtimeVehicleRepository repository,
    TransitService transitService
  ) {
    this.repository = repository;
    this.transitService = transitService;
  }

  /**
   * Gets the realtime vehicles for a given pattern. If the pattern is a realtime-added one
   * then the original (scheduled) one is used for the lookup instead, so you receive the correct
   * result no matter if you use the realtime or static information.
   */
  @Override
  public List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern) {
    if (pattern.getOriginalTripPattern() != null) {
      pattern = pattern.getOriginalTripPattern();
    }
    return repository.getRealtimeVehicles(pattern);
  }

  @Override
  public OccupancyStatus getVehicleOccupancyStatus(Trip trip) {
    TripPattern pattern = transitService.findPattern(trip);
    if (pattern == null) {
      return NO_DATA_AVAILABLE;
    }
    return getRealtimeVehicles(pattern)
      .stream()
      .filter(vehicle -> trip.getId().equals(vehicle.trip().getId()))
      .max(Comparator.comparing(vehicle -> vehicle.time().orElse(Instant.MIN)))
      .flatMap(RealtimeVehicle::occupancyStatus)
      .orElse(NO_DATA_AVAILABLE);
  }
}
