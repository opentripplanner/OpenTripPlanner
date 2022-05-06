package org.opentripplanner.transit.model.calendar;

import gnu.trove.map.TIntObjectMap;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.trip.Timetable;

public class ServiceDay {

  private final TIntObjectMap<Timetable> timetablesByPatternIndex;

  public ServiceDay(TIntObjectMap<Timetable> timetablesByPatternIndex) {
    this.timetablesByPatternIndex = timetablesByPatternIndex;
  }

  @Nullable
  public Timetable timetable(int routablePatternIndex) {
    return timetablesByPatternIndex.get(routablePatternIndex);
  }
}
