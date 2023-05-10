package org.opentripplanner.netex.support.stoptime;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.elapsedTimeSinceMidnight;

import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface
 * for passing times comparison.
 * Passing times are exposed as seconds since midnight, taking into account the day offset.
 */
class RegularStopTimeAdaptor extends AbstractStopTimeAdaptor {

  protected RegularStopTimeAdaptor(TimetabledPassingTime timetabledPassingTime) {
    super(timetabledPassingTime);
  }

  @Override
  public boolean hasRegularStop() {
    return true;
  }

  @Override
  public boolean hasCompletePassingTime() {
    return hasArrivalTime() || hasDepartureTime();
  }

  @Override
  public boolean hasConsistentPassingTime() {
    return (
      arrivalTime() == null ||
      departureTime() == null ||
      normalizedDepartureTime() >= normalizedArrivalTime()
    );
  }

  @Override
  public int normalizedEarliestDepartureTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int normalizedLatestArrivalTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int normalizedDepartureTimeOrElseArrivalTime() {
    return hasDepartureTime() ? normalizedDepartureTime() : normalizedArrivalTime();
  }

  @Override
  public int normalizedArrivalTimeOrElseDepartureTime() {
    return hasArrivalTime() ? normalizedArrivalTime() : normalizedDepartureTime();
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset.
   */
  protected int normalizedDepartureTime() {
    return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset.
   */
  protected int normalizedArrivalTime() {
    return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
  }

  private boolean hasArrivalTime() {
    return arrivalTime() != null;
  }

  private boolean hasDepartureTime() {
    return departureTime() != null;
  }
}
