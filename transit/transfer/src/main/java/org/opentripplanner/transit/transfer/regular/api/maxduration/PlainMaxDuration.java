package org.opentripplanner.transit.transfer.regular.api.maxduration;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * The same max duration between all stops, for the walk and wheelchair profile types.
 *
 * @param maxDuration the longest transfer, measured with the user preferences of the profile
 */
public record PlainMaxDuration(Duration maxDuration) implements MaxDurationsPreference {
  public PlainMaxDuration {
    MaxDurationsPreference.requirePositive("max duration", maxDuration);
  }

  @Override
  public Duration longestDuration() {
    return maxDuration;
  }

  @Override
  public Optional<Duration> searchLimit(Set<AllowedMode> atSource) {
    return Optional.of(maxDuration);
  }

  @Override
  public boolean accepts(
    int durationInSeconds,
    Set<AllowedMode> atSource,
    Set<AllowedMode> atTarget
  ) {
    return durationInSeconds <= maxDuration.toSeconds();
  }
}
