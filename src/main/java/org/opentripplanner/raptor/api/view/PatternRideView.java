package org.opentripplanner.raptor.api.view;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

public interface PatternRideView<T extends RaptorTripSchedule, A extends ArrivalView<T>> {
  A prevArrival();
  int boardStopIndex();
  int boardPos();
  int boardTime();
  T trip();
  int relativeCost();
  int boardCost();
}
