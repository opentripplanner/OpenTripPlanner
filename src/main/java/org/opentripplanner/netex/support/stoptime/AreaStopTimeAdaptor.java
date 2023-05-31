package org.opentripplanner.netex.support.stoptime;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.elapsedTimeSinceMidnight;

import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface
 * for passing times comparison.
 * Passing times are exposed as seconds since midnight, taking into account the day offset.
 */
final class AreaStopTimeAdaptor extends AbstractStopTimeAdaptor {

  AreaStopTimeAdaptor(TimetabledPassingTime timetabledPassingTime) {
    super(timetabledPassingTime);
  }

  @Override
  public boolean isComplete() {
    return hasLatestArrivalTime() && hasEarliestDepartureTime();
  }

  @Override
  public boolean isConsistent() {
    return normalizedLatestArrivalTime() >= normalizedEarliestDepartureTime();
  }

  @Override
  public int normalizedEarliestDepartureTime() {
    return elapsedTimeSinceMidnight(earliestDepartureTime(), earliestDepartureDayOffset());
  }

  @Override
  public int normalizedLatestArrivalTime() {
    return elapsedTimeSinceMidnight(latestArrivalTime(), latestArrivalDayOffset());
  }

  @Override
  public int normalizedDepartureTimeOrElseArrivalTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int normalizedArrivalTimeOrElseDepartureTime() {
    throw new UnsupportedOperationException();
  }

  private boolean hasLatestArrivalTime() {
    return latestArrivalTime() != null;
  }

  private boolean hasEarliestDepartureTime() {
    return earliestDepartureTime() != null;
  }
}
