package org.opentripplanner.transit.model.calendar;

import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.trip.RoutingTripPattern;
import org.opentripplanner.transit.model.trip.Timetable;
import org.opentripplanner.transit.model.trip.TripOnDay;

public class PatternOnDay {

  private final OperatingDay day;
  private final RoutingTripPattern pattern;
  private final Timetable timetable;

  // TODO RTM  - Add next/prev

  public PatternOnDay(OperatingDay day, RoutingTripPattern pattern, Timetable timetable) {
    this.day = day;
    this.pattern = pattern;
    this.timetable = timetable;
  }

  public OperatingDay day() {
    return day;
  }

  public RoutingTripPattern pattern() {
    return pattern;
  }

  public Timetable timetable() {
    return timetable;
  }

  public RaptorTripScheduleSearch<TripOnDay> createBoardSearch() {
    return new TripScheduleSearchOnDays(this);
  }

  public RaptorTripScheduleSearch<TripOnDay> createAlightSearch() {
    return new TripScheduleSearchOnDays(this);
  }
}
