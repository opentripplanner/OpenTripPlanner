package org.opentripplanner.ext.carpooling.constraints;

import java.time.Duration;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that inserting a new passenger does not cause excessive delays
 * for existing passengers in a carpool trip.
 * <p>
 * Ensures that no existing passenger experiences:
 * - More than {@code maxDelay} additional wait time at their pickup location
 * - More than {@code maxDelay} later arrival at their dropoff location
 * <p>
 * This protects the rider experience by preventing situations where accepting
 * one more passenger significantly inconveniences existing bookings.
 */
public class PassengerDelayConstraints {

  private static final Logger LOG = LoggerFactory.getLogger(PassengerDelayConstraints.class);

  /**
   * Default maximum delay: 5 minutes.
   * No existing passenger should wait more than 5 minutes longer or arrive
   * more than 5 minutes later due to a new passenger insertion.
   */
  public static final Duration DEFAULT_MAX_DELAY = Duration.ofMinutes(5);

  private final Duration maxDelay;

  /**
   * Creates constraints with default 5-minute maximum delay.
   */
  public PassengerDelayConstraints() {
    this(DEFAULT_MAX_DELAY);
  }

  /**
   * Creates constraints with custom maximum delay.
   *
   * @param maxDelay Maximum acceptable delay for existing passengers
   */
  public PassengerDelayConstraints(Duration maxDelay) {
    this.maxDelay = DurationUtils.requireNonNegative(maxDelay);
  }

  /**
   * Checks if a passenger insertion satisfies delay constraints.
   *
   * @param originalCumulativeDurations Cumulative duration to each point in original route
   * @param modifiedCumulativeDurations Cumulative duration to each point in modified route
   * @param pickupPos Position where passenger pickup is inserted (1-indexed)
   * @param dropoffPos Position where passenger dropoff is inserted (1-indexed)
   * @return true if all existing passengers experience acceptable delays
   */
  public boolean satisfiesConstraints(
    Duration[] originalCumulativeDurations,
    Duration[] modifiedCumulativeDurations,
    int pickupPos,
    int dropoffPos
  ) {
    // If no existing stops (only boarding and alighting), no constraint to check
    if (originalCumulativeDurations.length <= 2) {
      return true;
    }

    // Check delay at each existing stop (exclude boarding at 0 and alighting at end)
    for (
      int originalIndex = 1;
      originalIndex < originalCumulativeDurations.length - 1;
      originalIndex++
    ) {
      int modifiedIndex = InsertionPosition.mapOriginalIndex(originalIndex, pickupPos, dropoffPos);

      Duration originalTime = originalCumulativeDurations[originalIndex];
      Duration modifiedTime = modifiedCumulativeDurations[modifiedIndex];
      Duration delay = modifiedTime.minus(originalTime);

      if (delay.compareTo(maxDelay) > 0) {
        LOG.debug(
          "Insertion rejected: stop at position {} delayed by {}s (max: {}s)",
          originalIndex,
          delay.getSeconds(),
          maxDelay.getSeconds()
        );
        return false;
      }

      LOG.trace(
        "Stop at position {} delay: {}s (acceptable, max: {}s)",
        originalIndex,
        delay.getSeconds(),
        maxDelay.getSeconds()
      );
    }

    return true;
  }

  /**
   * Gets the configured maximum delay.
   *
   * @return Maximum delay duration
   */
  public Duration getMaxDelay() {
    return maxDelay;
  }
}
