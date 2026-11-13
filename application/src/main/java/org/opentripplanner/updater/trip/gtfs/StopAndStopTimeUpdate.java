package org.opentripplanner.updater.trip.gtfs;

import java.util.OptionalLong;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;

/**
 * A stop time update paired with its stop, constructed while validating the stops.
 *
 * @param extraCall                  {@code true} if the stop is not part of the scheduled trip but
 *                                   was added by a real-time replacement of the trip.
 * @param fallbackScheduledArrival   scheduled arrival time (seconds since epoch) of the matching
 *                                   stop in the scheduled trip, if there is one.
 * @param fallbackScheduledDeparture scheduled departure time (seconds since epoch) of the matching
 *                                   stop in the scheduled trip, if there is one.
 */
record StopAndStopTimeUpdate(
  StopLocation stop,
  StopTimeUpdate stopTimeUpdate,
  boolean extraCall,
  OptionalLong fallbackScheduledArrival,
  OptionalLong fallbackScheduledDeparture
) {
  StopAndStopTimeUpdate(StopLocation stop, StopTimeUpdate stopTimeUpdate) {
    this(stop, stopTimeUpdate, false);
  }

  StopAndStopTimeUpdate(StopLocation stop, StopTimeUpdate stopTimeUpdate, boolean extraCall) {
    this(stop, stopTimeUpdate, extraCall, OptionalLong.empty(), OptionalLong.empty());
  }

  OptionalLong scheduledArrival() {
    var arrival = stopTimeUpdate.scheduledArrivalTimeWithRealTimeFallback();
    if (arrival.isPresent() || !stopTimeUpdate.isNoData()) {
      return arrival;
    }
    return fallbackScheduledArrival;
  }

  OptionalLong scheduledDeparture() {
    var departure = stopTimeUpdate.scheduledDepartureTimeWithRealTimeFallback();
    if (departure.isPresent() || !stopTimeUpdate.isNoData()) {
      return departure;
    }
    return fallbackScheduledDeparture;
  }
}
