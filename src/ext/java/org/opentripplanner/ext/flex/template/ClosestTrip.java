package org.opentripplanner.ext.flex.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

/**
 * The combination of the closest stop and trip with active dates where the trip is in service.
 *
 * @param activeDates This is a mutable list, when building an instance the
 *                    {@link #addDate(FlexServiceDate)} can be used to add dates to the list.
 */
public class ClosestTrip {

  private final NearbyStop nearbyStop;
  private final FlexTrip<?, ?> flexTrip;
  private final int stopPos;
  private final List<FlexServiceDate> activeDates = new ArrayList<>();

  private ClosestTrip(NearbyStop nearbyStop, FlexTrip<?, ?> flexTrip, int stopPos) {
    this.nearbyStop = nearbyStop;
    this.flexTrip = flexTrip;
    this.stopPos = stopPos;
  }

  /** This method is static, so we can move it to the FlexTemplateFactory later. */
  public static Collection<ClosestTrip> of(
    FlexAccessEgressCallbackService callbackService,
    Collection<NearbyStop> nearbyStops,
    List<FlexServiceDate> dates,
    boolean pickup
  ) {
    Map<FlexTrip<?, ?>, ClosestTrip> map = new HashMap<>();
    // Find all trips reachable from the nearbyStops
    for (NearbyStop nearbyStop : nearbyStops) {
      var stop = nearbyStop.stop;
      for (var trip : callbackService.getFlexTripsByStop(stop)) {
        int stopPos = pickup ? trip.findBoardIndex(stop) : trip.findAlightIndex(stop);
        if (stopPos != FlexTrip.STOP_INDEX_NOT_FOUND) {
          var existing = map.get(trip);
          if (
            existing == null ||
            (
              nearbyStop.state.getElapsedTimeSeconds() <
              existing.nearbyStop().state.getElapsedTimeSeconds()
            )
          ) {
            map.put(trip, new ClosestTrip(nearbyStop, trip, stopPos));
          }
        }
      }
    }

    // Add active dates
    for (Map.Entry<FlexTrip<?, ?>, ClosestTrip> e : map.entrySet()) {
      var closestTrip = e.getValue();
      // Include dates where the service is running
      dates
        .stream()
        .filter(date -> callbackService.isDateActive(date, e.getKey()))
        .forEach(closestTrip::addDate);
    }
    // Filter inactive trips and return
    return map.values().stream().filter(ClosestTrip::hasActiveDates).toList();
  }

  public NearbyStop nearbyStop() {
    return nearbyStop;
  }

  public FlexTrip<?, ?> flexTrip() {
    return flexTrip;
  }

  public int stopPos() {
    return stopPos;
  }

  public Iterable<FlexServiceDate> activeDates() {
    return activeDates;
  }

  public void addDate(FlexServiceDate date) {
    activeDates.add(date);
  }

  public boolean hasActiveDates() {
    return !activeDates.isEmpty();
  }
}
