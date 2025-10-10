package org.opentripplanner.ext.carpooling.constraints;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
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
    if (maxDelay.isNegative()) {
      throw new IllegalArgumentException("maxDelay must be non-negative");
    }
    this.maxDelay = maxDelay;
  }

  /**
   * Checks if a passenger insertion satisfies delay constraints.
   *
   * @param originalCumulativeTimes Cumulative duration to each point in original route
   * @param modifiedSegments Route segments after passenger insertion
   * @param pickupPos Position where passenger pickup is inserted (1-indexed)
   * @param dropoffPos Position where passenger dropoff is inserted (1-indexed)
   * @return true if all existing passengers experience acceptable delays
   */
  public boolean satisfiesConstraints(
    Duration[] originalCumulativeTimes,
    List<GraphPath<State, Edge, Vertex>> modifiedSegments,
    int pickupPos,
    int dropoffPos
  ) {
    // If no existing stops (only boarding and alighting), no constraint to check
    if (originalCumulativeTimes.length <= 2) {
      return true;
    }

    // Calculate cumulative times for modified route
    Duration[] modifiedTimes = new Duration[modifiedSegments.size() + 1];
    modifiedTimes[0] = Duration.ZERO;
    for (int i = 0; i < modifiedSegments.size(); i++) {
      GraphPath<State, Edge, Vertex> segment = modifiedSegments.get(i);
      Duration segmentDuration = Duration.between(
        segment.states.getFirst().getTime(),
        segment.states.getLast().getTime()
      );
      modifiedTimes[i + 1] = modifiedTimes[i].plus(segmentDuration);
    }

    // Check delay at each existing stop (exclude boarding at 0 and alighting at end)
    for (
      int originalIndex = 1;
      originalIndex < originalCumulativeTimes.length - 1;
      originalIndex++
    ) {
      int modifiedIndex = getModifiedIndex(originalIndex, pickupPos, dropoffPos);

      Duration originalTime = originalCumulativeTimes[originalIndex];
      Duration modifiedTime = modifiedTimes[modifiedIndex];
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
   * Maps an index in the original route to the corresponding index in the
   * modified route after passenger stops have been inserted.
   *
   * @param originalIndex Index in original route
   * @param pickupPos Position where pickup was inserted (1-indexed)
   * @param dropoffPos Position where dropoff was inserted (1-indexed, in original space)
   * @return Corresponding index in modified route
   */
  private int getModifiedIndex(int originalIndex, int pickupPos, int dropoffPos) {
    int modifiedIndex = originalIndex;

    // Account for pickup insertion
    // If the original point was at or after pickupPos, it shifts by 1
    if (originalIndex >= pickupPos) {
      modifiedIndex++;
    }

    // Account for dropoff insertion
    // After pickup insertion, check if the shifted index is at or after dropoffPos
    if (modifiedIndex >= dropoffPos) {
      modifiedIndex++;
    }

    return modifiedIndex;
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
