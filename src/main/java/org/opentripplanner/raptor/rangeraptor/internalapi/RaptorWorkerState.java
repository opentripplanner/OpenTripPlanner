package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.Iterator;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.DefaultRangeRaptorWorker;
import org.opentripplanner.raptor.spi.IntIterator;

/**
 * The contract the state must implement for the {@link DefaultRangeRaptorWorker} to do its job. This
 * allows us to mix workers and states to implement different versions of the algorithm like
 * Standard, Standard-reversed and multi-criteria and use this with different states keeping only
 * the information needed by the use-case. Some example use-cases are calculating heuristics,
 * debugging and returning result paths.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorWorkerState<T extends RaptorTripSchedule> {
  /** Used to signal iteration termination, no more paths can be found for this iteration. */
  boolean isNewRoundAvailable();

  /** List all stops visited last round. */
  IntIterator stopsTouchedPreviousRound();

  /** Return a list of stops visited by transit, before doing transfers. */
  IntIterator stopsTouchedByTransitCurrentRound();

  /**
   * Return TRUE if at least one new destination arrival is accepted at the destination in the
   * current round. If no paths to the destination is found in the current round, FALSE is returned.
   * And last, if a new path is found in the current round - reaching the destination - but the path
   * is NOT accepted(not pareto-optimal), then FALSE is returned.
   * <p/>
   * This method is called at the end of each round.
   */
  boolean isDestinationReachedInCurrentRound();

  /**
   * Return TRUE if a stop is reached by transit or transfer in the previous round.
   */
  boolean isStopReachedInPreviousRound(int stopIndex);

  /**
   * Update state with a new transfer.
   */
  void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers);

  RaptorWorkerResult<T> results();
}
