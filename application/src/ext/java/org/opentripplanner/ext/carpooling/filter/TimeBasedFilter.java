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

  /**
   * Default time window: ±30 minutes from requested time.
   * Trips departing outside this window are rejected.
   */
  public static final Duration DEFAULT_TIME_WINDOW = Duration.ofMinutes(30);

  private final Duration timeWindow;

  public TimeBasedFilter() {
    this(DEFAULT_TIME_WINDOW);
  }

  public TimeBasedFilter(Duration timeWindow) {
    this.timeWindow = timeWindow;
  }

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    // Cannot filter without time information
    LOG.warn(
      "TimeBasedFilter called without time parameter - accepting all trips. " +
      "Use accepts(..., Instant) instead."
    );
    return true;
  }

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Instant passengerDepartureTime
  ) {
    Instant tripStartTime = trip.startTime().toInstant();

    // Calculate time difference
    Duration timeDiff = Duration.between(tripStartTime, passengerDepartureTime).abs();

    // Check if within time window
    boolean withinWindow = timeDiff.compareTo(timeWindow) <= 0;

    if (!withinWindow) {
      LOG.debug(
        "Trip {} rejected by time filter: trip departs at {}, passenger requests {}, diff = {} (window = {})",
        trip.getId(),
        trip.startTime(),
        passengerDepartureTime,
        timeDiff,
        timeWindow
      );
    }

    return withinWindow;
  }

  /**
   * Gets the configured time window.
   */
  public Duration getTimeWindow() {
    return timeWindow;
  }
}
