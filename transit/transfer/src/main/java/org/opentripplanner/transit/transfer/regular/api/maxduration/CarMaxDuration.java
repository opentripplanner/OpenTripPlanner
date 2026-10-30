package org.opentripplanner.transit.transfer.regular.api.maxduration;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * The max duration of the car profile type. A car can only be used between stops that allow cars,
 * for example the stops of a car ferry, so there are only transfers from and to such stops. No
 * street search is needed from the other stops.
 *
 * @param maxDuration the longest transfer, measured with the user preferences of the profile
 */
public record CarMaxDuration(Duration maxDuration) implements MaxDurationsPreference {
  public CarMaxDuration {
    MaxDurationsPreference.requirePositive("max duration", maxDuration);
  }

  @Override
  public Duration longestDuration() {
    return maxDuration;
  }

  @Override
  public Optional<Duration> searchLimit(Set<AllowedMode> atSource) {
    return atSource.contains(AllowedMode.CAR) ? Optional.of(maxDuration) : Optional.empty();
  }

  @Override
  public boolean accepts(
    int durationInSeconds,
    Set<AllowedMode> atSource,
    Set<AllowedMode> atTarget
  ) {
    return (
      atSource.contains(AllowedMode.CAR) &&
      atTarget.contains(AllowedMode.CAR) &&
      durationInSeconds <= maxDuration.toSeconds()
    );
  }
}
