package org.opentripplanner.transit.transfer.regular.api.maxduration;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The max durations of the bicycle profile type. A bicycle can cover a long distance, so the
 * transfers between all stops are limited to a short duration, and are allowed to be longer between
 * stops that allow bicycles or cars, for example the stops of a ferry. Both stops of a transfer
 * must allow the mode. The limits are independent of each other, and the longest that applies to a
 * transfer is used.
 * <p>
 * At least one of the limits is required. If there is no limit for all stops, there are only
 * transfers between the stops that allow bicycles or cars, as set by the other limits. Each limit
 * of the stops that allow a mode must be longer than the limit for all stops.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 *
 * @param maxDurationAllowAllModes the longest transfer between any two stops, or {@code null} if
 *                                 there are no transfers between stops that are not allowed for
 *                                 the modes below
 * @param maxDuration              the longest transfer between two stops that allow bicycles, or
 *                                 {@code null} if there is no limit for these stops
 * @param maxDurationAllowsCar     the longest transfer between two stops that allow cars, or
 *                                 {@code null} if there is no limit for these stops
 */
public record BicycleMaxDurations(
  @Nullable Duration maxDurationAllowAllModes,
  @Nullable Duration maxDuration,
  @Nullable Duration maxDurationAllowsCar
) implements MaxDurationsPreference {
  public BicycleMaxDurations {
    if (maxDurationAllowAllModes == null && maxDuration == null && maxDurationAllowsCar == null) {
      throw new IllegalArgumentException("At least one bicycle max duration is required.");
    }
    requirePositiveIfSet("maxDurationAllowAllModes", maxDurationAllowAllModes);
    requirePositiveIfSet("maxDuration", maxDuration);
    requirePositiveIfSet("maxDurationAllowsCar", maxDurationAllowsCar);
    requireLonger("maxDuration", maxDuration, maxDurationAllowAllModes);
    requireLonger("maxDurationAllowsCar", maxDurationAllowsCar, maxDurationAllowAllModes);
  }

  private static void requirePositiveIfSet(String name, @Nullable Duration duration) {
    if (duration != null) {
      MaxDurationsPreference.requirePositive(name, duration);
    }
  }

  /** The limit of a mode must be longer than the limit for all stops, if both are set. */
  private static void requireLonger(
    String name,
    @Nullable Duration duration,
    @Nullable Duration allModes
  ) {
    if (duration != null && allModes != null && duration.compareTo(allModes) <= 0) {
      throw new IllegalArgumentException(
        "The " +
          name +
          " must be longer than the maxDurationAllowAllModes (" +
          allModes +
          "): " +
          duration
      );
    }
  }

  @Override
  public Duration longestDuration() {
    return limits(true, true).max(Comparator.naturalOrder()).orElseThrow();
  }

  @Override
  public Optional<Duration> searchLimit(Set<AllowedMode> atSource) {
    return limits(atSource.contains(AllowedMode.BICYCLE), atSource.contains(AllowedMode.CAR)).max(
      Comparator.naturalOrder()
    );
  }

  @Override
  public boolean accepts(
    int durationInSeconds,
    Set<AllowedMode> atSource,
    Set<AllowedMode> atTarget
  ) {
    return (
      isWithin(durationInSeconds, maxDurationAllowAllModes) ||
      (bothAllow(AllowedMode.BICYCLE, atSource, atTarget) &&
        isWithin(durationInSeconds, maxDuration)) ||
      (bothAllow(AllowedMode.CAR, atSource, atTarget) &&
        isWithin(durationInSeconds, maxDurationAllowsCar))
    );
  }

  /** The limits that apply to a stop that allows bicycles and/or cars. */
  private Stream<Duration> limits(boolean allowsBicycle, boolean allowsCar) {
    return Stream.of(
      maxDurationAllowAllModes,
      allowsBicycle ? maxDuration : null,
      allowsCar ? maxDurationAllowsCar : null
    ).filter(d -> d != null);
  }

  private static boolean bothAllow(
    AllowedMode mode,
    Set<AllowedMode> atSource,
    Set<AllowedMode> atTarget
  ) {
    return atSource.contains(mode) && atTarget.contains(mode);
  }

  private static boolean isWithin(int durationInSeconds, @Nullable Duration limit) {
    return limit != null && durationInSeconds <= limit.toSeconds();
  }
}
