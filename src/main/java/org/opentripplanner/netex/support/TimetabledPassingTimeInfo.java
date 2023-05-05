package org.opentripplanner.netex.support;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.elapsedTimeSinceMidnight;

import java.util.Map;
import java.util.Objects;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface
 * for passing times comparison.
 * Passing times are exposed as seconds since midnight, taking into account the day offset.
 */
public class TimetabledPassingTimeInfo {

  /**
   * Map a timetabledPassingTime to true if its stop is a stop area, false otherwise.
   */
  private final Map<TimetabledPassingTime, Boolean> stopFlexibility;
  private final TimetabledPassingTime timetabledPassingTime;

  public TimetabledPassingTimeInfo(
    TimetabledPassingTime timetabledPassingTime,
    Map<TimetabledPassingTime, Boolean> stopFlexibility
  ) {
    this.stopFlexibility = stopFlexibility;
    this.timetabledPassingTime = timetabledPassingTime;
  }

  public boolean hasAreaStop() {
    return stopFlexibility.get(timetabledPassingTime);
  }

  public boolean hasRegularStop() {
    return !hasAreaStop();
  }

  /**
   * A passing time on a regular stop is complete if either arrival or departure time is present. A
   * passing time on an area stop is complete if both earliest departure time and latest arrival
   * time are present.
   */
  public boolean hasCompletePassingTime() {
    if (hasRegularStop()) {
      return (
        timetabledPassingTime.getArrivalTime() != null ||
        timetabledPassingTime.getDepartureTime() != null
      );
    }
    return (
      timetabledPassingTime.getLatestArrivalTime() != null &&
      timetabledPassingTime.getEarliestDepartureTime() != null
    );
  }

  /**
   * A passing time on a regular stop is consistent if departure time is after arrival time. A
   * passing time on an area stop is consistent if latest arrival time is after earliest departure
   * time.
   */
  public boolean hasConsistentPassingTime() {
    if (
      hasRegularStop() &&
      (
        timetabledPassingTime.getArrivalTime() == null ||
        timetabledPassingTime.getDepartureTime() == null
      )
    ) {
      return true;
    }
    if (
      hasRegularStop() &&
      timetabledPassingTime.getArrivalTime() != null &&
      timetabledPassingTime.getDepartureTime() != null
    ) {
      return (normalizedDepartureTime() >= normalizedArrivalTime());
    } else {
      return (normalizedLatestArrivalTime() >= normalizedEarliestDepartureTime());
    }
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset.
   */
  public int normalizedDepartureTime() {
    Objects.requireNonNull(timetabledPassingTime.getDepartureTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getDepartureTime(),
      timetabledPassingTime.getDepartureDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset.
   */
  public int normalizedArrivalTime() {
    Objects.requireNonNull(timetabledPassingTime.getArrivalTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getArrivalTime(),
      timetabledPassingTime.getArrivalDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the earliest departure time, taking into
   * account the day offset.
   */
  public int normalizedEarliestDepartureTime() {
    Objects.requireNonNull(timetabledPassingTime.getEarliestDepartureTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getEarliestDepartureTime(),
      timetabledPassingTime.getEarliestDepartureDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the latest arrival time, taking into
   * account the day offset.
   */
  public int normalizedLatestArrivalTime() {
    Objects.requireNonNull(timetabledPassingTime.getLatestArrivalTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getLatestArrivalTime(),
      timetabledPassingTime.getLatestArrivalDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset. Fallback to arrival time if departure time is missing.
   */
  public int normalizedDepartureTimeOrElseArrivalTime() {
    if (timetabledPassingTime.getDepartureTime() != null) {
      return elapsedTimeSinceMidnight(
        timetabledPassingTime.getDepartureTime(),
        timetabledPassingTime.getDepartureDayOffset()
      );
    } else {
      return elapsedTimeSinceMidnight(
        timetabledPassingTime.getArrivalTime(),
        timetabledPassingTime.getArrivalDayOffset()
      );
    }
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset. Fallback to departure time if arrival time is missing.
   */
  public int normalizedArrivalTimeOrElseDepartureTime() {
    if (timetabledPassingTime.getArrivalTime() != null) {
      return elapsedTimeSinceMidnight(
        timetabledPassingTime.getArrivalTime(),
        timetabledPassingTime.getArrivalDayOffset()
      );
    } else return elapsedTimeSinceMidnight(
      timetabledPassingTime.getDepartureTime(),
      timetabledPassingTime.getDepartureDayOffset()
    );
  }

  public Object getId() {
    return timetabledPassingTime.getId();
  }
}
