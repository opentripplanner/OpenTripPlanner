package org.opentripplanner.transit.transfer.regular.api.maxduration;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * The max durations of the bicycle profile type. A bicycle can cover a long distance, so the
 * transfers are limited to a short duration between all stops, and are allowed to be longer between
 * stops that allow bicycles, for example the stops of a ferry that carries bicycles. Both stops
 * of a transfer must allow the mode.
 * <p>
 * The durations of the transfers between all stops, between stops that allow bicycles, and
 * between stops that allow both bicycles and cars are increasing.
 *
 * @param maxDuration                    the longest transfer between any two stops
 * @param maxDurationAllowsBicycle       the longest transfer between two stops that allow
 *                                       bicycles, or {@code null} if there is no longer duration
 *                                       for these stops
 * @param maxDurationAllowsBicycleAndCar the longest transfer between two stops that allow both
 *                                       bicycles and cars (not just one of them), or {@code null}
 *                                       if there is no longer duration for these stops
 */
public record BicycleMaxDurations(
  Duration maxDuration,
  @Nullable Duration maxDurationAllowsBicycle,
  @Nullable Duration maxDurationAllowsBicycleAndCar
) implements MaxDurationsPreference {
  private static final Set<AllowedMode> BICYCLE_AND_CAR = Collections.unmodifiableSet(
    EnumSet.of(AllowedMode.BICYCLE, AllowedMode.CAR)
  );

  public BicycleMaxDurations {
    MaxDurationsPreference.requirePositive("max duration", maxDuration);
    var previous = maxDuration;
    var previousName = "maxDuration";
    if (maxDurationAllowsBicycle != null) {
      requireLonger("maxDurationAllowsBicycle", maxDurationAllowsBicycle, previousName, previous);
      previous = maxDurationAllowsBicycle;
      previousName = "maxDurationAllowsBicycle";
    }
    if (maxDurationAllowsBicycleAndCar != null) {
      requireLonger(
        "maxDurationAllowsBicycleAndCar",
        maxDurationAllowsBicycleAndCar,
        previousName,
        previous
      );
    }
  }

  public BicycleMaxDurations(Duration maxDuration) {
    this(maxDuration, null, null);
  }

  @Override
  public Optional<Duration> searchLimit(Set<AllowedMode> atSource) {
    return Optional.of(limit(atSource, atSource));
  }

  @Override
  public boolean accepts(
    int durationInSeconds,
    Set<AllowedMode> atSource,
    Set<AllowedMode> atTarget
  ) {
    return durationInSeconds <= limit(atSource, atTarget).toSeconds();
  }

  private static void requireLonger(
    String name,
    Duration duration,
    String shorterName,
    Duration shorter
  ) {
    if (duration.compareTo(shorter) <= 0) {
      throw new IllegalArgumentException(
        "The " +
          name +
          " must be longer than the " +
          shorterName +
          " (" +
          shorter +
          "): " +
          duration
      );
    }
  }

  /** The longest duration of a transfer between two stops that allow the given modes. */
  private Duration limit(Set<AllowedMode> atSource, Set<AllowedMode> atTarget) {
    if (maxDurationAllowsBicycleAndCar != null && allowsBoth(atSource) && allowsBoth(atTarget)) {
      return maxDurationAllowsBicycleAndCar;
    }
    if (
      maxDurationAllowsBicycle != null &&
      atSource.contains(AllowedMode.BICYCLE) &&
      atTarget.contains(AllowedMode.BICYCLE)
    ) {
      return maxDurationAllowsBicycle;
    }
    return maxDuration;
  }

  private static boolean allowsBoth(Set<AllowedMode> modes) {
    return modes.containsAll(BICYCLE_AND_CAR);
  }
}
