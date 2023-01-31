package org.opentripplanner.transit.model.calendar;

import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.trip.TripOnDay;

public class DefaultTripScheduleSearch implements RaptorTripScheduleSearch<TripOnDay> {

  @Override
  public RaptorBoardOrAlightEvent<TripOnDay> search(
    int earliestBoardTime,
    int stopPositionInPattern
  ) {
    return RaptorTripScheduleSearch.super.search(earliestBoardTime, stopPositionInPattern);
  }

  @Override
  public RaptorBoardOrAlightEvent<TripOnDay> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    return null;
  }
}
