package org.opentripplanner.netex.support;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.elapsedTimeSinceMidnight;

import java.math.BigInteger;
import java.time.LocalTime;
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
  private final boolean stopIsFlexibleArea;
  private final TimetabledPassingTime timetabledPassingTime;

  private TimetabledPassingTimeInfo(
    TimetabledPassingTime timetabledPassingTime,
    boolean stopIsFlexibleArea
  ) {
    this.stopIsFlexibleArea = stopIsFlexibleArea;
    this.timetabledPassingTime = timetabledPassingTime;
  }

  public static TimetabledPassingTimeInfo of(
    TimetabledPassingTime timetabledPassingTime,
    boolean stopIsFlexibleArea
  ) {
    return new TimetabledPassingTimeInfo(timetabledPassingTime, stopIsFlexibleArea);
  }

  public boolean hasAreaStop() {
    return stopIsFlexibleArea;
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
      return hasArrivalTime() || hasDepartureTime();
    }
    return (hasLatestArrivalTime() && hasEarliestDepartureTime());
  }

  /**
   * A passing time on a regular stop is consistent if departure time is after arrival time. A
   * passing time on an area stop is consistent if latest arrival time is after earliest departure
   * time.
   */
  public boolean hasConsistentPassingTime() {
    if (hasRegularStop() && (arrivalTime() == null || departureTime() == null)) {
      return true;
    }
    if (hasRegularStop() && hasArrivalTime() && hasDepartureTime()) {
      return normalizedDepartureTime() >= normalizedArrivalTime();
    } else {
      return normalizedLatestArrivalTime() >= normalizedEarliestDepartureTime();
    }
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset.
   */
  public int normalizedDepartureTime() {
    Objects.requireNonNull(departureTime());
    return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset.
   */
  public int normalizedArrivalTime() {
    Objects.requireNonNull(arrivalTime());
    return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the earliest departure time, taking into
   * account the day offset.
   */
  public int normalizedEarliestDepartureTime() {
    Objects.requireNonNull(earliestDepartureTime());
    return elapsedTimeSinceMidnight(earliestDepartureTime(), earliestDepartureDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the latest arrival time, taking into
   * account the day offset.
   */
  public int normalizedLatestArrivalTime() {
    Objects.requireNonNull(latestArrivalTime());
    return elapsedTimeSinceMidnight(latestArrivalTime(), latestArrivalDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset. Fallback to arrival time if departure time is missing.
   */
  public int normalizedDepartureTimeOrElseArrivalTime() {
    if (hasDepartureTime()) {
      return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
    } else {
      return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
    }
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset. Fallback to departure time if arrival time is missing.
   */
  public int normalizedArrivalTimeOrElseDepartureTime() {
    if (hasArrivalTime()) {
      return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
    } else return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
  }

  public Object getId() {
    return timetabledPassingTime.getId();
  }

  private LocalTime arrivalTime() {
    return timetabledPassingTime.getArrivalTime();
  }

  private boolean hasArrivalTime() {
    return arrivalTime() != null;
  }

  private BigInteger arrivalDayOffset() {
    return timetabledPassingTime.getArrivalDayOffset();
  }

  private LocalTime latestArrivalTime() {
    return timetabledPassingTime.getLatestArrivalTime();
  }

  private boolean hasLatestArrivalTime() {
    return latestArrivalTime() != null;
  }

  private BigInteger latestArrivalDayOffset() {
    return timetabledPassingTime.getLatestArrivalDayOffset();
  }

  private LocalTime departureTime() {
    return timetabledPassingTime.getDepartureTime();
  }

  private boolean hasDepartureTime() {
    return departureTime() != null;
  }

  private BigInteger departureDayOffset() {
    return timetabledPassingTime.getDepartureDayOffset();
  }

  private LocalTime earliestDepartureTime() {
    return timetabledPassingTime.getEarliestDepartureTime();
  }

  private boolean hasEarliestDepartureTime() {
    return earliestDepartureTime() != null;
  }

  private BigInteger earliestDepartureDayOffset() {
    return timetabledPassingTime.getEarliestDepartureDayOffset();
  }
}
