package org.opentripplanner.transit.model.plan;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.calendar.TripScheduleSearchOnDays;
import org.opentripplanner.transit.model.trip.TripOnDate;

public record RaptorRouteAdaptor(
  RaptorTripPattern routingPattern,
  TripScheduleSearchOnDays timetables
)
  implements
    RaptorRoute<TripOnDate>, RaptorTimeTable<TripOnDate>, RaptorTripScheduleSearch<TripOnDate> {
  /* implement RaptorRoute */

  @Override
  public RaptorTimeTable<TripOnDate> timetable() {
    return this;
  }

  @Override
  public RaptorTripPattern pattern() {
    return routingPattern;
  }

  @Override
  public RaptorBoardOrAlightEvent<TripOnDate> search(
    int earliestBoardTime,
    int stopPositionInPattern
  ) {
    return null;
  }

  /* implement RaptorTimeTable */

  @Override
  public TripOnDate getTripSchedule(int index) {
    // TODO RTM
    return null;
  }

  @Override
  public int numberOfTripSchedules() {
    // TODO RTM -
    return 0;
  }

  @Override
  public RaptorTripScheduleSearch<TripOnDate> tripSearch(SearchDirection direction) {
    //    return switch (direction) {
    //      case FORWARD -> new DepartureTripSearch(timetables,)
    //      case REVERSE ->
    //    };
    return null;
  }

  /* implement RaptorTripScheduleSearch<> */

  @Override
  public RaptorBoardOrAlightEvent<TripOnDate> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    return null;
  }
}
