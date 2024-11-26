package org.opentripplanner.raptor.rangeraptor.standard;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;

/**
 * This interface define the methods used be the {@link ArrivalTimeRoutingStrategy} to query and
 * update the state of the algorithm.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface StdWorkerState<T extends RaptorTripSchedule> extends RaptorWorkerState<T> {
  /**
   * Return the best time at the given stop found in the last round. This is used to find the right
   * trip to board in the current round.
   * <p/>
   * If you are not trying to find paths or calculate the exact number of transfers it is ok to
   * return the overall best tim to reach the given stop.
   */
  int bestTimePreviousRound(int stop);

  /**
   * See {@link org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy#setAccessToStop(RaptorAccessEgress, int)}.
   */
  void setAccessToStop(RaptorAccessEgress accessPath, int departureTime);

  /**
   * Set the time at a transit stop iff it is optimal. This sets both the bestTime and the
   * transitTime
   */
  void transitToStop(int alightStop, int alightTime, int boardStop, int boardTime, T trip);

  TransitArrival<T> previousTransit(int boardStopIndex);
}
