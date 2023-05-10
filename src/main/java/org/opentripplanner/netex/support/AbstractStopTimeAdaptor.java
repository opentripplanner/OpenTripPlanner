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
public class AbstractStopTimeAdaptor implements StopTimeAdaptor {

  /**
   * Map a timetabledPassingTime to true if its stop is a stop area, false otherwise.
   */
  private final boolean stopIsFlexibleArea;
  private final TimetabledPassingTime timetabledPassingTime;

  private AbstractStopTimeAdaptor(
    TimetabledPassingTime timetabledPassingTime,
    boolean stopIsFlexibleArea
  ) {
    this.stopIsFlexibleArea = stopIsFlexibleArea;
    this.timetabledPassingTime = timetabledPassingTime;
  }

  public static AbstractStopTimeAdaptor of(
    TimetabledPassingTime timetabledPassingTime,
    boolean stopIsFlexibleArea
  ) {
    return new AbstractStopTimeAdaptor(timetabledPassingTime, stopIsFlexibleArea);
  }

  @Override
  public boolean hasAreaStop() {
    return stopIsFlexibleArea;
  }

  @Override
  public boolean hasRegularStop() {
    return !hasAreaStop();
  }

  @Override
  public boolean hasCompletePassingTime() {
    if (hasRegularStop()) {
      return hasArrivalTime() || hasDepartureTime();
    }
    return (hasLatestArrivalTime() && hasEarliestDepartureTime());
  }

  @Override
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

  @Override
  public int normalizedEarliestDepartureTime() {
    Objects.requireNonNull(earliestDepartureTime());
    return elapsedTimeSinceMidnight(earliestDepartureTime(), earliestDepartureDayOffset());
  }

  @Override
  public int normalizedLatestArrivalTime() {
    Objects.requireNonNull(latestArrivalTime());
    return elapsedTimeSinceMidnight(latestArrivalTime(), latestArrivalDayOffset());
  }

  @Override
  public int normalizedDepartureTimeOrElseArrivalTime() {
    if (hasDepartureTime()) {
      return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
    } else {
      return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
    }
  }

  @Override
  public int normalizedArrivalTimeOrElseDepartureTime() {
    if (hasArrivalTime()) {
      return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
    } else return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
  }

  @Override
  public Object timetabledPassingTimeId() {
    return timetabledPassingTime.getId();
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset.
   */
  protected int normalizedDepartureTime() {
    Objects.requireNonNull(departureTime());
    return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset.
   */
  protected int normalizedArrivalTime() {
    Objects.requireNonNull(arrivalTime());
    return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
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
