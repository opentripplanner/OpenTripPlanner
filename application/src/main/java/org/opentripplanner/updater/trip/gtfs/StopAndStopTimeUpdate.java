package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;

/**
 * A stop time update paired with its stop, constructed while validating the stops.
 *
 * @param extraCall {@code true} if the stop is not part of the scheduled trip but was added by a
 *                  real-time replacement of the trip.
 */
record StopAndStopTimeUpdate(StopLocation stop, StopTimeUpdate stopTimeUpdate, boolean extraCall) {
  StopAndStopTimeUpdate(StopLocation stop, StopTimeUpdate stopTimeUpdate) {
    this(stop, stopTimeUpdate, false);
  }
}
