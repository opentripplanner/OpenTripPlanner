package org.opentripplanner.netex.support.stoptime;

import java.math.BigInteger;
import java.time.LocalTime;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface
 * for passing times comparison.
 * Passing times are exposed as seconds since midnight, taking into account the day offset.
 */
abstract class AbstractStopTimeAdaptor implements StopTimeAdaptor {

  private final TimetabledPassingTime timetabledPassingTime;

  protected AbstractStopTimeAdaptor(TimetabledPassingTime timetabledPassingTime) {
    this.timetabledPassingTime = timetabledPassingTime;
  }

  @Override
  public final boolean hasAreaStop() {
    return !hasRegularStop();
  }

  @Override
  public final Object timetabledPassingTimeId() {
    return timetabledPassingTime.getId();
  }

  protected LocalTime arrivalTime() {
    return timetabledPassingTime.getArrivalTime();
  }

  protected BigInteger arrivalDayOffset() {
    return timetabledPassingTime.getArrivalDayOffset();
  }

  protected LocalTime latestArrivalTime() {
    return timetabledPassingTime.getLatestArrivalTime();
  }

  protected BigInteger latestArrivalDayOffset() {
    return timetabledPassingTime.getLatestArrivalDayOffset();
  }

  protected LocalTime departureTime() {
    return timetabledPassingTime.getDepartureTime();
  }

  protected BigInteger departureDayOffset() {
    return timetabledPassingTime.getDepartureDayOffset();
  }

  protected LocalTime earliestDepartureTime() {
    return timetabledPassingTime.getEarliestDepartureTime();
  }

  protected BigInteger earliestDepartureDayOffset() {
    return timetabledPassingTime.getEarliestDepartureDayOffset();
  }
}
