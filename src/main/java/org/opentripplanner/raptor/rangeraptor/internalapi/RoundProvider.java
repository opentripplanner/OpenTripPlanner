package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.rangeraptor.transit.RoundTracker;

/**
 * Keep track of current Raptor round. The provider is injected where needed instead of passing the
 * current round down the call stack. This is faster than passing the round on the stack because the
 * round is access so frequently thet in most cases it is cached in the CPU registry - at least
 * tests indicate this.
 * <p/>
 *
 * @see RoundTracker
 */
public interface RoundProvider {
  /**
   * The current Raptor round.
   */
  int round();
}
