package org.opentripplanner.ext.carpooling;

import java.time.Duration;

/**
 * The limits and heuristic values of the carpooling routing, collected in one place.
 * <p>
 * These are deployment-wide values, not part of the routing request. They are hard-coded to
 * {@link #DEFAULT} for now; the intention is to expose them in a {@code carpooling} section of
 * {@code router-config.json}, so this is the type that mapping would produce.
 *
 * @param maxTripDuration the longest span a carpool trip may have, from the first stop's departure
 *        to the destination's latest expected arrival; a longer trip is not shaped like a carpool
 *        journey and is dropped, and no car search reaches further
 * @param tripExpiry how long a trip is kept after its latest end time, since the feed is not
 *        guaranteed to cancel a journey once it has completed
 * @param expirySweepInterval the least time between two sweeps of the expired trips; trips expire
 *        on a multi-day timescale, so sweeping on every poll would be wasted work
 * @param maxRoutePointSnap the reach of the search that moves a driver's route point onto the
 *        drivable network when it is not on a car-reachable vertex already; a point farther away
 *        is unresolvable
 * @param minCarEscapeMeters how far, in a straight line, a car must be able to drive to and from a
 *        vertex for it to count as car-reachable: far enough to clear a connected vertex, short
 *        enough to reject a stranded stub or island
 * @param defaultSearchWindow the search window of a request that has none; wide, because carpool
 *        trips are sparse and a narrow window would lose matches the passenger would accept
 */
public record CarpoolingParameters(
  Duration maxTripDuration,
  Duration tripExpiry,
  Duration expirySweepInterval,
  Duration maxRoutePointSnap,
  double minCarEscapeMeters,
  Duration defaultSearchWindow
) {
  public static final CarpoolingParameters DEFAULT = new CarpoolingParameters(
    Duration.ofHours(2).plusMinutes(30),
    Duration.ofDays(2),
    Duration.ofHours(1),
    Duration.ofMinutes(5),
    500,
    Duration.ofMinutes(300)
  );

  public CarpoolingParameters {
    requirePositive(maxTripDuration, "maxTripDuration");
    requirePositive(tripExpiry, "tripExpiry");
    requirePositive(expirySweepInterval, "expirySweepInterval");
    requirePositive(maxRoutePointSnap, "maxRoutePointSnap");
    if (minCarEscapeMeters <= 0) {
      throw new IllegalArgumentException("minCarEscapeMeters must be positive");
    }
    requirePositive(defaultSearchWindow, "defaultSearchWindow");
  }

  private static void requirePositive(Duration value, String name) {
    if (value.isNegative() || value.isZero()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
