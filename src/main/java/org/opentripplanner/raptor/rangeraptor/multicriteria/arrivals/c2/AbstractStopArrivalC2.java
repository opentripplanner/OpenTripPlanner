package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * Abstract super class for multi-criteria stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
abstract class AbstractStopArrivalC2<T extends RaptorTripSchedule> extends McStopArrival<T> {

  private final int c2;

  /**
   * Transit or transfer.
   *
   * @param previous             the previous arrival visited for the current trip
   * @param paretoRoundIncrement the increment to add to the paretoRound
   * @param stop                 stop index for this arrival
   * @param arrivalTime          the arrival time for this stop index
   * @param c1                   first criteria, the total accumulated generalized-cost-1 criteria
   * @param c2                   second criteria, the total accumulated generalized-cost-2 criteria
   */
  AbstractStopArrivalC2(
    McStopArrival<T> previous,
    int paretoRoundIncrement,
    int stop,
    int arrivalTime,
    int c1,
    int c2
  ) {
    super(previous, paretoRoundIncrement, stop, arrivalTime, c1);
    this.c2 = c2;
  }

  /**
   * Initial state - first stop visited during the RAPTOR algorithm.
   */
  AbstractStopArrivalC2(
    int stop,
    int departureTime,
    int travelDuration,
    int paretoRound,
    int c1,
    int c2
  ) {
    super(stop, departureTime, travelDuration, c1, paretoRound);
    this.c2 = c2;
  }

  public final int c2() {
    return c2;
  }

  @Override
  public String toString() {
    return asString();
  }
}
