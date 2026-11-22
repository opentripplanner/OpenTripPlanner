package org.opentripplanner.updater.trip.siri;

import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Configuration of the {@link SiriFuzzyTripMatcher} for a single SIRI-ET updater.
 * <p>
 * The fuzzy trip matcher identifies a scheduled trip for a SIRI message without a usable trip id
 * by comparing the last stop, the line, the service date and the aimed times of the first and the
 * last call. Some producers send aimed times rounded to whole minutes, while the schedule contains
 * seconds. The {@code timeTolerance} makes it possible to match these trips: a scheduled time
 * matches if it differs from the aimed time by at most the tolerance.
 *
 * @param enabled       If {@code false}, no fuzzy trip matching is done and the time tolerance
 *                      must be zero.
 * @param timeTolerance The maximum difference between the scheduled and the aimed time, in whole
 *                      seconds and less than 30 minutes. Zero means that the times must be equal
 *                      to the second.
 */
public record SiriFuzzyTripMatchingParameters(boolean enabled, Duration timeTolerance) {
  /** Fuzzy trip matching is switched off. */
  public static final SiriFuzzyTripMatchingParameters DISABLED =
    new SiriFuzzyTripMatchingParameters(false, Duration.ZERO);

  /** Fuzzy trip matching where the scheduled and the aimed times must be equal to the second. */
  public static final SiriFuzzyTripMatchingParameters EXACT = new SiriFuzzyTripMatchingParameters(
    true,
    Duration.ZERO
  );

  public SiriFuzzyTripMatchingParameters {
    Objects.requireNonNull(timeTolerance);
    DurationUtils.requireNonNegativeMax30minutes(timeTolerance, "timeTolerance");
    if (timeTolerance.getNano() != 0) {
      throw new IllegalArgumentException(
        "The timeTolerance must be in whole seconds: " + timeTolerance
      );
    }
    if (!enabled && !timeTolerance.isZero()) {
      throw new IllegalArgumentException(
        "The timeTolerance must be zero if fuzzy trip matching is disabled: " + timeTolerance
      );
    }
  }

  /** Enable fuzzy trip matching with the given time tolerance. */
  public static SiriFuzzyTripMatchingParameters ofTimeTolerance(Duration timeTolerance) {
    return new SiriFuzzyTripMatchingParameters(true, timeTolerance);
  }

  /**
   * Map the legacy boolean configuration: {@code true} is fuzzy matching with zero tolerance,
   * {@code false} is no fuzzy matching.
   */
  public static SiriFuzzyTripMatchingParameters of(boolean enabled) {
    return enabled ? EXACT : DISABLED;
  }

  @Override
  public String toString() {
    if (!enabled) {
      return "SiriFuzzyTripMatchingParameters{disabled}";
    }
    return ToStringBuilder.of(SiriFuzzyTripMatchingParameters.class)
      .addDuration("timeTolerance", timeTolerance)
      .toString();
  }
}
