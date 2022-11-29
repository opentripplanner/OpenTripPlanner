package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public interface StopArrivalState<T extends RaptorTripSchedule> {
  static <T extends RaptorTripSchedule> StopArrivalState<T> create() {
    return new DefaultStopArrivalState<>();
  }

  /** The overall best time to reach this stop */
  int time();

  /**
   * The best time to reach this stop on board a vehicle, it may be by transit or by flex access.
   */
  int onBoardArrivalTime();

  /** Stop arrival reached by transit or on-board access. */
  boolean reachedOnBoard();

  /* Access */

  /** Stop arrival reached, at least one time (any round/iteration). */
  boolean reachedOnStreet();

  /**
   * Return true is the best option is an access arrival.
   */
  boolean arrivedByAccessOnStreet();

  /**
   * Return the access path for the best stop arrival.
   */
  RaptorAccessEgress accessPathOnStreet();

  /**
   * Return true is the best option is an access arrival.
   */
  boolean arrivedByAccessOnBoard();

  /* Transit */

  /**
   * Return the access path for the best stop arrival.
   */
  RaptorAccessEgress accessPathOnBoard();

  /**
   * A transit arrival exist, but it might be a better transfer arrival as well.
   */
  boolean arrivedByTransit();

  T trip();

  int boardTime();

  int boardStop();

  void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip);

  /* Transfer */

  void setBestTimeTransit(int time);

  /**
   * The best arrival is by transfer.
   */
  boolean arrivedByTransfer();

  int transferFromStop();

  RaptorTransfer transferPath();

  /**
   * Set the time at a transit index iff it is optimal. This sets both the best time and the
   * transfer time
   */
  void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath);
}
