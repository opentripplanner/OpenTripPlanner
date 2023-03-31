package org.opentripplanner.raptor.api.view;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * TODO C2 AddJavaDoc
 *
 * @param <T>
 */
public interface PatternRideView<T extends RaptorTripSchedule, A extends ArrivalView<T>> {
  A prevArrival();
  int boardStopIndex();
  int boardPos();
  int boardTime();
  T trip();
  int relativeC1();
  int boardC1();
  int c2();
}
