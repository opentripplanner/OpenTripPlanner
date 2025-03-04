package org.opentripplanner.ext.flex.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * The combination of the closest stop, trip and trip active date.
 */
record ClosestTrip(
  NearbyStop nearbyStop,
  FlexTrip<?, ?> flexTrip,
  int stopPos,
  FlexServiceDate activeDate
) {
  ClosestTrip(
    NearbyStop nearbyStop,
    FlexTrip<?, ?> flexTrip,
    int stopPos,
    FlexServiceDate activeDate
  ) {
    this.nearbyStop = Objects.requireNonNull(nearbyStop);
    this.flexTrip = Objects.requireNonNull(flexTrip);
    this.stopPos = IntUtils.requireNotNegative(stopPos, "stopPos");
    this.activeDate = activeDate;
  }

  /**
   * Create a temporary closest-trip without an active-date
   */
  private ClosestTrip(NearbyStop nearbyStop, FlexTrip<?, ?> flexTrip, int stopPos) {
    this(nearbyStop, flexTrip, stopPos, null);
  }

  private ClosestTrip(ClosestTrip original, FlexServiceDate activeDate) {
    this(original.nearbyStop, original.flexTrip, original.stopPos, activeDate);
  }

  /**
   * Create a set of the closest trips running on the dates provided. Only the
   * combination of the closest nearby-stop and trip is kept. For each combination,
   * the set of dates is checked, and an instance with each active date is returned.
   */
  static Collection<ClosestTrip> of(
    FlexAccessEgressCallbackAdapter callbackService,
    Collection<NearbyStop> nearbyStops,
    List<FlexServiceDate> dates,
    boolean pickup
  ) {
    var closestTrips = findAllTripsReachableFromNearbyStop(callbackService, nearbyStops, pickup);
    return findActiveDatesForTripAndDecorateResult(callbackService, dates, closestTrips, true);
  }

  @Override
  public FlexServiceDate activeDate() {
    // The active date is not required as an internal "trick" to create closest-trips
    // in two steps, but the instance is not valid before the active-date is added. This
    // method should not be used inside this class, only on fully constructed valid instances;
    // Hence the active-date should not be null.
    return Objects.requireNonNull(activeDate);
  }

  private static Map<FlexTrip<?, ?>, ClosestTrip> findAllTripsReachableFromNearbyStop(
    FlexAccessEgressCallbackAdapter callbackService,
    Collection<NearbyStop> nearbyStops,
    boolean pickup
  ) {
    var map = new HashMap<FlexTrip<?, ?>, ClosestTrip>();
    for (NearbyStop nearbyStop : nearbyStops) {
      var stop = nearbyStop.stop;
      for (var trip : callbackService.getFlexTripsByStop(stop)) {
        int stopPos = pickup ? trip.findBoardIndex(stop) : trip.findAlightIndex(stop);
        if (stopPos != FlexTrip.STOP_INDEX_NOT_FOUND) {
          var existing = map.get(trip);
          if (existing == null || nearbyStop.isBetter(existing.nearbyStop())) {
            map.put(trip, new ClosestTrip(nearbyStop, trip, stopPos));
          }
        }
      }
    }
    return map;
  }

  private static ArrayList<ClosestTrip> findActiveDatesForTripAndDecorateResult(
    FlexAccessEgressCallbackAdapter callbackService,
    List<FlexServiceDate> dates,
    Map<FlexTrip<?, ?>, ClosestTrip> map,
    boolean pickup
  ) {
    var result = new ArrayList<ClosestTrip>();
    // Add active dates
    for (Map.Entry<FlexTrip<?, ?>, ClosestTrip> e : map.entrySet()) {
      var trip = e.getKey();
      var closestTrip = e.getValue();
      // Include dates where the service is running
      for (FlexServiceDate date : dates) {
        // Filter away boardings early. This needs to be done for egress as well when the
        // board stop is known (not known here).
        if (pickup && exceedsLatestBookingTime(trip, date, closestTrip.stopPos())) {
          continue;
        }
        if (callbackService.isDateActive(date, trip)) {
          result.add(closestTrip.withDate(date));
        }
      }
    }
    return result;
  }

  private ClosestTrip withDate(FlexServiceDate date) {
    Objects.requireNonNull(date);
    return new ClosestTrip(this, date);
  }

  /**
   * Check if the trip can be booked at the given date and boarding stop position.
   */
  private static boolean exceedsLatestBookingTime(
    FlexTrip<?, ?> trip,
    FlexServiceDate date,
    int stopPos
  ) {
    return RoutingBookingInfo.of(
      date.requestedBookingTime(),
      trip.getPickupBookingInfo(stopPos)
    ).exceedsLatestBookingTime();
  }
}
