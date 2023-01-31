package org.opentripplanner.transit.model.plan;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.calendar.PatternOnDay;
import org.opentripplanner.transit.model.trip.TripOnDay;

public class RaptorRouteAdaptor implements RaptorRoute<TripOnDay>, RaptorTimeTable<TripOnDay> {

  private final PatternOnDay pattern;

  public RaptorRouteAdaptor(PatternOnDay pattern) {
    this.pattern = pattern;
  }

  /* implement RaptorRoute */

  @Override
  public RaptorTimeTable<TripOnDay> timetable() {
    return this;
  }

  @Override
  public RaptorTripPattern pattern() {
    return pattern.pattern();
  }

  /* implement RaptorTimeTable */

  @Override
  public TripOnDay getTripSchedule(int index) {
    return new TripOnDay(index, pattern.timetable());
  }

  @Override
  public int numberOfTripSchedules() {
    return pattern.timetable().numOfTrips();
  }

  @Override
  public RaptorTripScheduleSearch<TripOnDay> tripSearch(SearchDirection direction) {
    // TODO RTM -

    return null;
  }
}
