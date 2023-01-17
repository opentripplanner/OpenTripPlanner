package org.opentripplanner.transit.model.calendar;

import org.opentripplanner.transit.model.trip.RoutingTripPattern;
import org.opentripplanner.transit.model.trip.Timetable;

public class PatternForDay {

  private final OperatingDay day;
  private final RoutingTripPattern pattern;
  private final Timetable timetable;

  public PatternForDay(OperatingDay day, RoutingTripPattern pattern, Timetable timetable) {
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
}
