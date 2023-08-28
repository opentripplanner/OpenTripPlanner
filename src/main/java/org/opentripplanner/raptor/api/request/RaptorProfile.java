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
  MULTI_CRITERIA("Mc", true),

  /**
   * Used by Range Raptor finding the earliest-arrival-time, the shortest travel duration and the
   * fewest transfers. Generalized-cost is not used.
   * <p/>
   * Computes result paths.
   */
  STANDARD("Standard", true),

  /**
   * Same as {@link #STANDARD}, but no paths are computed/returned.
   */
  BEST_TIME("StdBestTime", false),

  /**
   * Used by Raptor to find the shortest travel duration ignoring wait-time. It also finds number
   * transfers. This profile can only be used with one Raptor iteration - no {code searchWindow}.
   * The path is not kept, because this potentially creates paths which is not possible; Hence,
   * can not be constructed.
   */
  MIN_TRAVEL_DURATION("MinTravelDuration", true);

  private final boolean supportsConstrainedTransfers;

  private final String abbreviation;

  RaptorProfile(String abbreviation, boolean supportsConstrainedTransfers) {
    this.supportsConstrainedTransfers = supportsConstrainedTransfers;
    this.abbreviation = abbreviation;
  }

  public final String abbreviation() {
    return abbreviation;
  }

  public boolean supportsConstrainedTransfers() {
    return supportsConstrainedTransfers;
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
    return is(MIN_TRAVEL_DURATION);
  }

  public boolean producesGeneralizedCost() {
    return is(MULTI_CRITERIA);
  }
}
