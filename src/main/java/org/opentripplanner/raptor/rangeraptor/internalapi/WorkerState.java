package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.Collection;
import java.util.Iterator;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.RangeRaptorWorker;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * The contract the state must implement for the {@link RangeRaptorWorker} to do its job. This allow
 * us to mix workers and states to implement different versions of the algorithm like Standard,
 * Standard-reversed and multi-criteria and use this with different states keeping only the
 * information needed by the use-case. Some example use-cases are calculating heuristics, debugging
 * and returning result paths.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface WorkerState<T extends RaptorTripSchedule> {
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
   * Add access path to state. This should be called in the matching round and appropriate place in
   * the algorithm according to the {@link RaptorTransfer#numberOfRides()} and {@link
   * RaptorTransfer#stopReachedOnBoard()}.
   */
  void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime);

  /**
   * Update state with a new transfer.
   */
  void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers);

  /**
   * Extract paths after the search is complete. This method is optional, returning an empty set by
   * default.
   *
   * @return return all paths found in the search.
   */
  Collection<Path<T>> extractPaths();

  /**
   * Get arrival statistics for each stop reached in the search.
   */
  StopArrivals extractStopArrivals();
}
