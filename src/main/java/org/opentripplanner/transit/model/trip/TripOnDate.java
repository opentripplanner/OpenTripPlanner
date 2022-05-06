package org.opentripplanner.transit.model.trip;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * TODO RTM - THIS IS A PLACEHOLDER for {@link org.opentripplanner.model.TripTimeOnDate}
 */
public class TripOnDate implements RaptorTripSchedule {

  private final int tripIndex;
  private final Timetable timetable;

  public TripOnDate(int tripIndex, Timetable timetable) {
    this.tripIndex = tripIndex;
    this.timetable = timetable;
  }

  @Override
  public int tripSortIndex() {
    return 0;
  }

  @Override
  public int arrival(int stopPosInPattern) {
    return timetable.alightTime(tripIndex, stopPosInPattern);
  }

  @Override
  public int departure(int stopPosInPattern) {
    return timetable.boardTime(tripIndex, stopPosInPattern);
  }

  @Override
  public RaptorTripPattern pattern() {
    return null;
  }
}
