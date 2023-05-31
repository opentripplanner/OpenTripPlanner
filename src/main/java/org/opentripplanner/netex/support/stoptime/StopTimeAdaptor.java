package org.opentripplanner.netex.support.stoptime;

import org.rutebanken.netex.model.TimetabledPassingTime;

public sealed interface StopTimeAdaptor permits AbstractStopTimeAdaptor {
  static StopTimeAdaptor of(
    TimetabledPassingTime timetabledPassingTime,
    boolean stopIsFlexibleArea
  ) {
    return stopIsFlexibleArea
      ? new AreaStopTimeAdaptor(timetabledPassingTime)
      : new RegularStopTimeAdaptor(timetabledPassingTime);
  }

  /**
   * A passing time on a regular stop is complete if either arrival or departure time is present. A
   * passing time on an area stop is complete if both earliest departure time and latest arrival
   * time are present.
   */
  boolean isComplete();

  /**
   * A passing time on a regular stop is consistent if departure time is after arrival time. A
   * passing time on an area stop is consistent if latest arrival time is after earliest departure
   * time.
   */
  boolean isConsistent();

  /**
   * Return the elapsed time in second between midnight and the earliest departure time, taking into
   * account the day offset. Only valid for area-stops, throw an exception if not.
   */
  int normalizedEarliestDepartureTime();

  /**
   * Return the elapsed time in second between midnight and the latest arrival time, taking into
   * account the day offset. Only valid for area-stops, throw an exception if not.
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

  /**
   * Return {@code true} if this stop-time is before the given {@code next} stop time.
   */
  boolean isStopTimesIncreasing(StopTimeAdaptor next);
}
