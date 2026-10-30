package org.opentripplanner.transit.transfer.regular.api.maxduration;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

/**
 * How far from a stop the transfers of a {@link TransferProfileType} are searched. The limit
 * bounds the discovery search, that is, the street search made when the transfers are generated.
 * Re-costing a stored path under the preferences of a request never drops it.
 * <p>
 * The street search is made once from each stop, with the longest duration that applies to the
 * stop, see {@link #searchLimit}. Each path found is then accepted or dropped using
 * {@link #accepts}, since the duration that applies depends on the stop the path ends in too. The
 * variants of this interface are:
 * <ul>
 *   <li>{@link PlainMaxDuration}: the same duration between all stops, for walk and wheelchair.</li>
 *   <li>{@link BicycleMaxDurations}: a duration between all stops, and longer durations between
 *       stops that allow bicycles or cars.</li>
 *   <li>{@link CarMaxDuration}: transfers between stops that allow cars only.</li>
 * </ul>
 * <p>
 * Implementations are immutable and thread-safe.
 */
public sealed interface MaxDurationsPreference
  permits PlainMaxDuration, BicycleMaxDurations, CarMaxDuration
{
  PlainMaxDuration DEFAULT = new PlainMaxDuration(TransferParameters.DEFAULT_MAX_DURATION);

  /** The same max duration between all stops. */
  static PlainMaxDuration of(Duration maxDuration) {
    return new PlainMaxDuration(maxDuration);
  }

  /**
   * The default max duration of a profile type, {@link TransferParameters#DEFAULT_MAX_DURATION}, in
   * the way of the type.
   */
  static MaxDurationsPreference defaultFor(TransferProfileType type) {
    var thirtyMinutes = TransferParameters.DEFAULT_MAX_DURATION;
    return switch (type) {
      case WALK, WHEELCHAIR -> DEFAULT;
      case BICYCLE -> new BicycleMaxDurations(null, thirtyMinutes, null);
      case CAR -> new CarMaxDuration(thirtyMinutes);
    };
  }

  /** The longest of all the durations. */
  Duration longestDuration();

  /**
   * How far to search from a stop that allows the modes {@code atSource}: the longest duration of
   * any transfer from the stop. Empty if there are no transfers from the stop, in which case the
   * street search can be skipped.
   */
  Optional<Duration> searchLimit(Set<AllowedMode> atSource);

  /** Whether there are transfers from a stop that allows the modes {@code atSource}. */
  default boolean hasTransfersFrom(Set<AllowedMode> atSource) {
    return searchLimit(atSource).isPresent();
  }

  /**
   * Whether a transfer of the given duration is within the max durations, between stops that allow
   * the given modes. This is used to drop the paths of the discovery search that are too long for
   * the stops they connect.
   */
  boolean accepts(int durationInSeconds, Set<AllowedMode> atSource, Set<AllowedMode> atTarget);

  /** Check that the duration is positive. */
  static Duration requirePositive(String name, Duration duration) {
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException("The " + name + " must be positive: " + duration);
    }
    return duration;
  }
}
