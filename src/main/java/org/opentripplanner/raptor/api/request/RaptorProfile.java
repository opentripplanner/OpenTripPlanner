package org.opentripplanner.raptor.api.request;

import java.util.stream.Stream;
import org.opentripplanner.raptor.rangeraptor.standard.MinTravelDurationRoutingStrategy;

/**
 * Several implementation are implemented - with different behaviour. Use the one that suites your
 * need best.
 */
public enum RaptorProfile {
  /**
   * Multi criteria pareto search.
   */
  MULTI_CRITERIA("Mc"),

  /**
   * Used by Range Raptor finding the earliest-arrival-time, the shortest travel duration and the
   * fewest transfers. Generalized-cost is not used.
   * <p/>
   * Computes result paths.
   */
  STANDARD("Standard"),

  /**
   * Same as {@link #STANDARD}, but no paths are computed/returned.
   */
  BEST_TIME("StdBestTime"),

  /**
   * Used by Raptor to find the shortest travel duration ignoring wait-time. It also finds number
   * transfers. This profile can only be used with one Raptor iteration - no {code searchWindow}.
   */
  MIN_TRAVEL_DURATION("MinTravelDuration"),

  /**
   * Same as {@link #MIN_TRAVEL_DURATION}, but no paths are computed/returned.
   */
  MIN_TRAVEL_DURATION_BEST_TIME("MinTravelDurationBT");

  private final String abbreviation;

  RaptorProfile(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  public final String abbreviation() {
    return abbreviation;
  }

  public boolean is(RaptorProfile candidate) {
    return this == candidate;
  }

  public boolean isOneOf(RaptorProfile... candidates) {
    return Stream.of(candidates).anyMatch(this::is);
  }

  /**
   * The {@link MinTravelDurationRoutingStrategy} will time-shift the arrival-times, so we need to
   * use the approximate trip-times search in path construction. The BEST_TIME state should not have
   * path construction, but we include it here anyway.
   */
  public boolean useApproximateTripSearch() {
    return isOneOf(MIN_TRAVEL_DURATION, MIN_TRAVEL_DURATION_BEST_TIME);
  }
}
