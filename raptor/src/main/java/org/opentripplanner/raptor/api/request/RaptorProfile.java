package org.opentripplanner.raptor.api.request;

import java.util.stream.Stream;

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
   * Used by Raptor to find the shortest travel duration ignoring wait-time. It also finds the
   * number of transfers. This profile can only be used with one Raptor iteration - no
   * {code searchWindow}. The path is not kept because this potentially creates paths, which are
   * not possible; Hence, cannot be constructed.
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

  public boolean producesGeneralizedCost() {
    return is(MULTI_CRITERIA);
  }
}
