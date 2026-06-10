package org.opentripplanner.ext.carpooling.constraints;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that inserting a new passenger does not cause excessive delays
 * for existing stops in a carpool trip.
 * <p>
 * Ensures that no existing stop experiences more delay than its own
 * {@link CarpoolStop#getDeviationBudget() deviationBudget} allows.
 * <p>
 * This protects the rider experience by preventing situations where accepting
 * one more passenger significantly inconveniences existing bookings.
 */
public final class PassengerDelayConstraints {

  private static final Logger LOG = LoggerFactory.getLogger(PassengerDelayConstraints.class);

  private PassengerDelayConstraints() {}

  /**
   * Checks if a passenger insertion satisfies delay constraints for all existing stops.
   * Each stop is checked against its own deviation budget.
   *
   * @param originalCumulativeDurations Cumulative duration to each point in original route
   * @param modifiedCumulativeDurations Cumulative duration to each point in modified route
   * @param pickupPos 0-based index of the passenger's pickup in the modified route
   * @param dropoffPos 0-based index of the passenger's dropoff in the modified route
   * @param stops The ordered list of stops in the original trip
   * @return true if all existing stops experience acceptable delays
   */
  public static boolean satisfiesConstraints(
    Duration[] originalCumulativeDurations,
    Duration[] modifiedCumulativeDurations,
    int pickupPos,
    int dropoffPos,
    List<CarpoolStop> stops
  ) {
    // Check delay at each existing stop (exclude origin at index 0)
    for (
      int originalIndex = 1;
      originalIndex < originalCumulativeDurations.length;
      originalIndex++
    ) {
      int modifiedIndex = InsertionPosition.mapOriginalIndex(originalIndex, pickupPos, dropoffPos);

      Duration delay = modifiedCumulativeDurations[modifiedIndex].minus(
        originalCumulativeDurations[originalIndex]
      );
      Duration stopBudget = stops.get(originalIndex).getDeviationBudget();

      if (delay.compareTo(stopBudget) > 0) {
        LOG.debug(
          "Stop at position {} delayed by {}s exceeds budget of {}s",
          originalIndex,
          delay.getSeconds(),
          stopBudget.getSeconds()
        );
        return false;
      }
    }

    return true;
  }
}
