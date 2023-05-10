package org.opentripplanner.netex.support.stoptime;

import org.rutebanken.netex.model.TimetabledPassingTime;

public interface StopTimeAdaptor {
  static AbstractStopTimeAdaptor of(
    TimetabledPassingTime timetabledPassingTime,
    boolean stopIsFlexibleArea
  ) {
    return new AbstractStopTimeAdaptor(timetabledPassingTime, stopIsFlexibleArea);
  }

  /**
   * Return true if the stop is a flexible area.
   */
  boolean hasAreaStop();

  /**
   * Return true if the stop is a regular stop (used by both scheduled and flex services).
   */
  boolean hasRegularStop();

  /**
   * A passing time on a regular stop is complete if either arrival or departure time is present. A
   * passing time on an area stop is complete if both earliest departure time and latest arrival
   * time are present.
   */
  boolean hasCompletePassingTime();

  /**
   * A passing time on a regular stop is consistent if departure time is after arrival time. A
   * passing time on an area stop is consistent if latest arrival time is after earliest departure
   * time.
   */
  boolean hasConsistentPassingTime();

  /**
   * Return the elapsed time in second between midnight and the earliest departure time, taking into
   * account the day offset.
   */
  int normalizedEarliestDepartureTime();

  /**
   * Return the elapsed time in second between midnight and the latest arrival time, taking into
   * account the day offset.
   */
  int normalizedLatestArrivalTime();

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset. Fallback to arrival time if departure time is missing.
   */
  int normalizedDepartureTimeOrElseArrivalTime();

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset. Fallback to departure time if arrival time is missing.
   */
  int normalizedArrivalTimeOrElseDepartureTime();

  Object timetabledPassingTimeId();
}
