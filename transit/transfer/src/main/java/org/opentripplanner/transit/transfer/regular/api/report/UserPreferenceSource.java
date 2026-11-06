package org.opentripplanner.transit.transfer.regular.api.report;

/**
 * How a key of the cache came to be.
 */
public enum UserPreferenceSource {
  /**
   * The preferences of a profile, the Raptor transfers are created when the server starts.
   */
  PROFILE,
  /**
   * The preferences of a warmup entry, created when the server starts.
   */
  WARMUP,
  /**
   * The preferences of a request that is not a profile or a warmup entry.
   */
  ON_DEMAND,
}
