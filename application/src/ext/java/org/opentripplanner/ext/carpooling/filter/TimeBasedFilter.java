package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on departure or arrival time compatibility with passenger request.
 * <p>
 * Rejects trips that depart significantly before or after the passenger's requested departure or
 * arrival time.
 * <p>
 * For passenger requests where the passenger is searching for a departure time, this prevents
 * matching passengers with trips that have already departed or won't depart for hours.
 * <p>
 * For passengers searching for arrival time, it prevents matching passengers with trips that arrive
 * too late or too early.
 */
public class TimeBasedFilter implements TripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(TimeBasedFilter.class);

  @Override
  public boolean accepts(CarpoolTrip trip, CarpoolingRequest request, Duration searchWindow) {
    if (request.getRequestedDateTime() == null) {
      return true;
    }

    var utcZoneNormalized = request.getRequestedDateTime();
    if (request.isArriveByRequest()) {
      return withinWindow(
        trip.getId(),
        utcZoneNormalized,
        trip.endTime().toInstant(),
        searchWindow,
        "arrives"
      );
    } else {
      return withinWindow(
        trip.getId(),
        utcZoneNormalized,
        trip.startTime().toInstant(),
        searchWindow,
        "departs"
      );
    }
  }

  private boolean withinWindow(
    Object tripId,
    Instant passengerTime,
    Instant tripTime,
    Duration searchWindow,
    String verb
  ) {
    var timeDiff = Duration.between(tripTime, passengerTime).abs();
    boolean withinWindow = timeDiff.compareTo(searchWindow) <= 0;

    if (!withinWindow) {
      LOG.debug(
        "Trip {} rejected by time filter: trip {} at {}, passenger requests {}, diff = {} (window = {})",
        tripId,
        verb,
        tripTime,
        passengerTime,
        timeDiff,
        searchWindow
      );
    }
    return withinWindow;
  }
}
