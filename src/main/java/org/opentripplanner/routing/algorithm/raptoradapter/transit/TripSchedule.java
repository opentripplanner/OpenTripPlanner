package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultTripSchedule;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Extension of RaptorTripSchedule passed through Raptor searches to be able to retrieve the
 * original trip from the path when creating itineraries.
 */
public interface TripSchedule extends DefaultTripSchedule {
  LocalDate getServiceDate();

  /**
   * TODO OTP2 - Add JavaDoc
   */
  TripTimes getOriginalTripTimes();

  /**
   * TODO OTP2 - Add JavaDoc
   */
  TripPattern getOriginalTripPattern();

  /**
   * Return {@code true} if this trip is not based on a fixed schedule, but instead a frequency
   * based scheduled trip. The {@link #frequencyHeadwayInSeconds()} is only defined for such trips.
   */
  default boolean isFrequencyBasedTrip() {
    return false;
  }

  /**
   * Return the {@code headway} for a frequency based trip. {@code -999} is returned for "normal"
   * scheduled trips, but you should not relay on this. Instead, use the {@link
   * #isFrequencyBasedTrip()} method to determine the trip type.
   */
  default int frequencyHeadwayInSeconds() {
    return -999;
  }
}
