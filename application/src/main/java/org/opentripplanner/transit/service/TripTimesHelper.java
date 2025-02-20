package org.opentripplanner.transit.service;

import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Helper class for filtering TripTimes.
 */
public class TripTimesHelper {

  /**
   * Returns true if the {code tripTimes} should be considered cancelled and
   * {@code includeCancellation} is also {@code true}.
   * <p>
   * Always returns {@code true} if the trip times is deleted.
   */
  public static boolean skipByTripCancellation(TripTimes tripTimes, boolean includeCancellations) {
    if (tripTimes.isDeleted()) {
      return true;
    }

    return (
      (tripTimes.isCanceled() || tripTimes.getTrip().getNetexAlteration().isCanceledOrReplaced()) &&
      !includeCancellations
    );
  }
}
