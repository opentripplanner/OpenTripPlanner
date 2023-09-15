package org.opentripplanner.raptor.api.view;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * A pattern ride provide read-only access to a
 * {@link org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide}.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
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
