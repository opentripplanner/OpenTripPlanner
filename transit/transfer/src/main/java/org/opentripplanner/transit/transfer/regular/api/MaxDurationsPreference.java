package org.opentripplanner.transit.transfer.regular.api;

import java.time.Duration;
import java.util.Objects;

/**
 * How far from a stop the transfers of a {@link TransferProfileType} are searched. The limit
 * bounds the discovery search, that is, the street search made when the transfers are generated.
 * Re-costing a stored path under the preferences of a request never drops it.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 *
 * @param maxDuration the longest transfer found, measured with the user preferences of the profile
 */
public record MaxDurationsPreference(Duration maxDuration) {
  public static final MaxDurationsPreference DEFAULT = new MaxDurationsPreference(
    Duration.ofMinutes(30)
  );

  public MaxDurationsPreference {
    Objects.requireNonNull(maxDuration);
    if (maxDuration.isZero() || maxDuration.isNegative()) {
      throw new IllegalArgumentException("The max duration must be positive: " + maxDuration);
    }
  }

  public static MaxDurationsPreference of(Duration maxDuration) {
    return new MaxDurationsPreference(maxDuration);
  }
}
