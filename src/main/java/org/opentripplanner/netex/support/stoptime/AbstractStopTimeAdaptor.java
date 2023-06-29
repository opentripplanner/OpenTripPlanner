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
        return isRegularStopFollowedByRegularStopValid(next);
      } else {
        return isRegularStopFollowedByAreaStopValid(next);
      }
    } else {
      if (next instanceof RegularStopTimeAdaptor) {
        return isAreaStopFollowedByRegularStopValid(next);
      } else {
        return isAreaStopFollowedByAreaStopValid(next);
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

  private boolean isRegularStopFollowedByRegularStopValid(StopTimeAdaptor next) {
    return (
      normalizedDepartureTimeOrElseArrivalTime() <= next.normalizedArrivalTimeOrElseDepartureTime()
    );
  }

  private boolean isAreaStopFollowedByAreaStopValid(StopTimeAdaptor next) {
    int earliestDepartureTime = normalizedEarliestDepartureTime();
    int nextEarliestDepartureTime = next.normalizedEarliestDepartureTime();
    int latestArrivalTime = normalizedLatestArrivalTime();
    int nextLatestArrivalTime = next.normalizedLatestArrivalTime();

    return (
      earliestDepartureTime <= nextEarliestDepartureTime &&
      latestArrivalTime <= nextLatestArrivalTime
    );
  }

  private boolean isRegularStopFollowedByAreaStopValid(StopTimeAdaptor next) {
    return normalizedDepartureTimeOrElseArrivalTime() <= next.normalizedEarliestDepartureTime();
  }

  private boolean isAreaStopFollowedByRegularStopValid(StopTimeAdaptor next) {
    return normalizedLatestArrivalTime() <= next.normalizedArrivalTimeOrElseDepartureTime();
  }
}
