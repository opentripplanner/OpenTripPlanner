package org.opentripplanner.transit.raptor.api.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public interface PatternRideView<T extends RaptorTripSchedule> {
  ArrivalView<T> prevArrival();
  int boardStopIndex();
  int boardPos();
  int boardTime();
  T trip();
  int relativeCost();
  int boardCost();
}
