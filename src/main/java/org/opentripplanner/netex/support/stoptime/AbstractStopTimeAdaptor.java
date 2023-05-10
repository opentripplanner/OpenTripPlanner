package org.opentripplanner.netex.support.stoptime;

import java.math.BigInteger;
import java.time.LocalTime;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface for passing times
 * comparison. Passing times are exposed as seconds since midnight, taking into account the day
 * offset.
 * <p>
 * This class does not take Daylight Saving Time transitions into account, this is an error and
 * should be fixed. See https://github.com/opentripplanner/OpenTripPlanner/issues/5109
 */
abstract sealed class AbstractStopTimeAdaptor
  implements StopTimeAdaptor
  permits AreaStopTimeAdaptor, RegularStopTimeAdaptor {

  private final TimetabledPassingTime timetabledPassingTime;

  protected AbstractStopTimeAdaptor(TimetabledPassingTime timetabledPassingTime) {
    this.timetabledPassingTime = timetabledPassingTime;
  }

  @Override
  public final Object timetabledPassingTimeId() {
    return timetabledPassingTime.getId();
  }

  @Override
  public final boolean isStopTimesIncreasing(StopTimeAdaptor next) {
    // This can be replaced with pattern-matching or polymorphic inheritance, BUT as long as we
    // only have 4 cases the "if" keep the rules together and make it easier to read/get the hole
    // picture - so keep it together until more cases are added.
    if (this instanceof RegularStopTimeAdaptor) {
      if (next instanceof RegularStopTimeAdaptor) {
        // regular followed by regular stop
        return (
          normalizedDepartureTimeOrElseArrivalTime() <=
          next.normalizedArrivalTimeOrElseDepartureTime()
        );
      } else {
        // Regular followed by area stop
        return normalizedDepartureTimeOrElseArrivalTime() <= next.normalizedEarliestDepartureTime();
      }
    } else {
      if (next instanceof RegularStopTimeAdaptor) {
        // area followed by regular stop
        return normalizedLatestArrivalTime() <= next.normalizedArrivalTimeOrElseDepartureTime();
      } else {
        // Area followed by area stop
        return (
          (normalizedEarliestDepartureTime() < next.normalizedEarliestDepartureTime()) &&
          (normalizedLatestArrivalTime() < next.normalizedLatestArrivalTime())
        );
      }
    }
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
