package org.opentripplanner.raptor.rangeraptor.standard.internalapi;

/**
 * The responsibility of this interface is to allow the caller to check if the destination is
 * reached in the current Raptor round.
 */
@FunctionalInterface
public interface ArrivedAtDestinationCheck {
  /**
   * If the destination is reached, but the path is rejected as an optimal path at the destination
   * this method should return FALSE, only when a new optimal path is fund should the method return
   * TRUE.
   * <p>
   * Return TRUE if the destination was reached in the current round; if not false.
   */
  boolean arrivedAtDestinationCurrentRound();
}
