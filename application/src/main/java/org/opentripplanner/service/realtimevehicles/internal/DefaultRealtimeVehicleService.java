package org.opentripplanner.service.realtimevehicles.internal;

import static org.opentripplanner.transit.model.timetable.OccupancyStatus.NO_DATA_AVAILABLE;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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

  /**
   * This multimap is immutable and therefore thread-safe. It is updated using the copy-on-write
   * pattern so data races are avoided. This is re-enforced with the variable being volatile.
   */
  private volatile ImmutableListMultimap<TripPattern, RealtimeVehicle> vehicles =
    ImmutableListMultimap.of();

  private final TransitService transitService;

  @Inject
  public DefaultRealtimeVehicleService(TransitService transitService) {
    this.transitService = transitService;
  }

  public void setRealtimeVehiclesForFeed(
    String feedId,
    Multimap<TripPattern, RealtimeVehicle> updates
  ) {
    Multimap<TripPattern, RealtimeVehicle> temp = ArrayListMultimap.create();
    temp.putAll(vehicles);
    // remove all previous updates for this specific feed id
    vehicles.keys().stream().filter(p -> p.getFeedId().equals(feedId)).forEach(temp::removeAll);
    // transform keys and put all fresh updates into map
    updates.forEach((pattern, vehicles) -> {
      if (pattern.getOriginalTripPattern() != null) {
        pattern = pattern.getOriginalTripPattern();
      }
      temp.put(pattern, vehicles);
    });

    vehicles = ImmutableListMultimap.copyOf(temp);
  }

  /**
   * Gets the realtime vehicles for a given pattern. If the pattern is a realtime-added one
   * then the original (scheduled) one is used for the lookup instead, so you receive the correct
   * result no matter if you use the realtime or static information.
   *
   * @see DefaultRealtimeVehicleService#setRealtimeVehiclesForFeed(String, Multimap)
   */
  @Override
  public List<RealtimeVehicle> getRealtimeVehicles(TripPattern pattern) {
    if (pattern.getOriginalTripPattern() != null) {
      pattern = pattern.getOriginalTripPattern();
    }
    // the list is made immutable during insertion, so we can safely return them
    return vehicles.get(pattern);
  }

  @Override
  public OccupancyStatus getVehicleOccupancyStatus(Trip trip) {
    return getOccupancyStatus(trip.getId(), transitService.findPattern(trip));
  }

  /**
   * Get the latest occupancy status for a certain trip. Service contains all the vehicles that
   * exist in input feeds but doesn't store any historical data. This method is an alternative to
   * {@link #getVehicleOccupancyStatus(Trip)} and works even when {@link TransitService} is not
   * provided to the service.
   */
  public OccupancyStatus getOccupancyStatus(FeedScopedId tripId, TripPattern pattern) {
    return vehicles
      .get(pattern)
      .stream()
      .filter(vehicle -> tripId.equals(vehicle.trip().getId()))
      .max(Comparator.comparing(vehicle -> vehicle.time().orElse(Instant.MIN)))
      .flatMap(RealtimeVehicle::occupancyStatus)
      .orElse(NO_DATA_AVAILABLE);
  }
}
