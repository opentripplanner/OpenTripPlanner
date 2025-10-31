package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on departure time compatibility with passenger request.
 * <p>
 * Rejects trips that depart significantly before or after the passenger's
 * requested departure time. This prevents matching passengers with trips
 * that have already departed or won't depart for hours.
 */
public class TimeBasedFilter implements TripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(TimeBasedFilter.class);

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    return true;
  }

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Instant passengerDepartureTime,
    Duration searchWindow
  ) {
    Instant tripStartTime = trip.startTime().toInstant();

    Duration timeDiff = Duration.between(tripStartTime, passengerDepartureTime).abs();

    boolean withinWindow = timeDiff.compareTo(searchWindow) <= 0;

    if (!withinWindow) {
      LOG.debug(
        "Trip {} rejected by time filter: trip departs at {}, passenger requests {}, diff = {} (window = {})",
        trip.getId(),
        trip.startTime(),
        passengerDepartureTime,
        timeDiff,
        searchWindow
      );
    }

    return withinWindow;
  }
}
