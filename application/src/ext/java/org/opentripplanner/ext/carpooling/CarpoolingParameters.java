package org.opentripplanner.ext.carpooling;

import java.time.Duration;

/**
 * The limits of the carpooling routing, collected in one place.
 * <p>
 * These are deployment-wide values, not part of the routing request. They are hard-coded to
 * {@link #DEFAULT} for now; the intention is to expose them in a {@code carpooling} section of
 * {@code router-config.json}, so this is the type that mapping would produce.
 *
 * @param maxCandidateTripsPerRequest the most trips a request evaluates; when more pass the
 *        pre-filters, the ones whose route passes closest to the passenger are kept
 * @param maxCandidatesPerStop the most access/egress candidates a transit stop hands to Raptor;
 *        a stop with more keeps the first and the last car and one per slot of the search window
 * @param maxTrips the most trips an instance holds, over all feeds; further new trips are dropped
 * @param maxStopWalk the longest walk between a transit stop and the vertex where a car can stop
 *        for it; stops farther from any drivable street are not served
 */
public record CarpoolingParameters(
  int maxCandidateTripsPerRequest,
  int maxCandidatesPerStop,
  int maxTrips,
  Duration maxStopWalk
) {
  public static final CarpoolingParameters DEFAULT = new CarpoolingParameters(
    50,
    24,
    10_000,
    Duration.ofMinutes(15)
  );

  public CarpoolingParameters {
    if (maxCandidateTripsPerRequest < 1) {
      throw new IllegalArgumentException("maxCandidateTripsPerRequest must be positive");
    }
    if (maxCandidatesPerStop < 4) {
      throw new IllegalArgumentException("maxCandidatesPerStop must be at least 4");
    }
    if (maxTrips < 1) {
      throw new IllegalArgumentException("maxTrips must be positive");
    }
    if (maxStopWalk.isNegative() || maxStopWalk.isZero()) {
      throw new IllegalArgumentException("maxStopWalk must be positive");
    }
  }
}
