package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.Collection;
import java.util.Iterator;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.StopArrivalsAdaptor;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.ArrivedAtDestinationCheck;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.StopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;
import org.opentripplanner.raptor.util.BitSetIterator;

/**
 * Tracks the state of a standard Range Raptor search, specifically the best arrival times at each
 * transit stop at the end of a particular round, along with associated data to reconstruct paths
 * etc.
 * <p>
 * This is grouped into a separate class (rather than just having the fields in the raptor worker
 * class) because we want to separate the logic of maintaining stop arrival state and performing the
 * steps of the algorithm. This also make it possible to have more than one state implementation,
 * which have ben used in the past to test different memory optimizations.
 * <p>
 * Note that this represents the entire state of the Range Raptor search for all rounds. The {@code
 * stopArrivalsState} implementation can be swapped to achieve different results.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdRangeRaptorWorkerState<T extends RaptorTripSchedule>
  implements StdWorkerState<T> {

  /**
   * The best times to reach each stop, whether via a transfer or via transit directly. This is the
   * bare minimum to execute the algorithm.
   */
  private final BestTimes bestTimes;

  /**
   * Track the stop arrivals to be able to return some kind of result. Depending on the desired
   * result, different implementation is injected.
   */
  private final StopArrivalsState<T> stopArrivalsState;

  /**
   * The list of egress stops, can be used to terminate the search when the stops are reached.
   */
  private final ArrivedAtDestinationCheck arrivedAtDestinationCheck;

  /**
   * The calculator is used to calculate transit related times/events like access arrival time.
   */
  private final TransitCalculator<T> calculator;

  /**
   * create a BestTimes Range Raptor State for given context.
   */
  public StdRangeRaptorWorkerState(
    TransitCalculator<T> calculator,
    BestTimes bestTimes,
    StopArrivalsState<T> stopArrivalsState,
    ArrivedAtDestinationCheck arrivedAtDestinationCheck
  ) {
    this.calculator = calculator;
    this.bestTimes = bestTimes;
    this.stopArrivalsState = stopArrivalsState;
    this.arrivedAtDestinationCheck = arrivedAtDestinationCheck;
  }

  @Override
  public boolean isNewRoundAvailable() {
    return bestTimes.isCurrentRoundUpdated();
  }

  @Override
  public IntIterator stopsTouchedPreviousRound() {
    return bestTimes.stopsReachedLastRound();
  }

  @Override
  public BitSetIterator stopsTouchedByTransitCurrentRound() {
    return bestTimes.reachedByTransitCurrentRound();
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    return arrivedAtDestinationCheck.arrivedAtDestinationCurrentRound();
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    final int durationInSeconds = accessPath.durationInSeconds();
    final int stop = accessPath.stop();

    // The time of arrival at the given stop for the current iteration
    // (or departure time at the last stop if we search backwards).
    int arrivalTime = calculator.plusDuration(departureTime, durationInSeconds);

    if (exceedsTimeLimit(arrivalTime)) {
      return;
    }

    boolean reachedOnBoard =
      accessPath.stopReachedOnBoard() && newBestTransitArrivalTime(stop, arrivalTime);
    boolean bestTime = newOverallBestTime(stop, arrivalTime);

    if (reachedOnBoard || bestTime) {
      stopArrivalsState.setAccessTime(arrivalTime, accessPath, bestTime);
    } else {
      stopArrivalsState.rejectAccessTime(arrivalTime, accessPath);
    }
  }

  /**
   * Set the arrival time at all transit stop if time is optimal for the given list of transfers.
   */
  @Override
  public void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
    int arrivalTimeTransit = bestTimes.transitArrivalTime(fromStop);
    while (transfers.hasNext()) {
      transferToStop(arrivalTimeTransit, fromStop, transfers.next());
    }
  }

  @Override
  public Collection<Path<T>> extractPaths() {
    return stopArrivalsState.extractPaths();
  }

  @Override
  public boolean isStopReachedInPreviousRound(int stop) {
    return bestTimes.isStopReachedLastRound(stop);
  }

  /**
   * Return the "best time" found in the previous round. This is used to calculate the board/alight
   * time in the next round.
   * <p/>
   * PLEASE OVERRIDE!
   * <p/>
   * The implementation here is not correct - please override if you plan to use any result paths or
   * "rounds" as "number of transfers". The implementation is OK if the only thing you care about is
   * the "arrival time".
   */
  @Override
  public int bestTimePreviousRound(int stop) {
    // This is a simplification, *bestTimes* might get updated during the current round;
    // Hence leading to a new boarding from the same stop in the same round.
    // If we do not count rounds or track paths, this is OK. But be sure to override this
    // method with the best time from the previous round if you care about number of
    // transfers and results paths.

    return stopArrivalsState.bestTimePreviousRound(stop);
  }

  /**
   * Set the time at a transit stop iff it is optimal. This sets both the bestTime and the
   * transitTime.
   */
  @Override
  public void transitToStop(int stop, int arrivalTime, int boardStop, int boardTime, T trip) {
    if (exceedsTimeLimit(arrivalTime)) {
      return;
    }

    if (newBestTransitArrivalTime(stop, arrivalTime)) {
      // transitTimes upper bounds bestTimes
      final boolean newOverallBestTime = newOverallBestTime(stop, arrivalTime);
      stopArrivalsState.setNewBestTransitTime(
        stop,
        arrivalTime,
        trip,
        boardStop,
        boardTime,
        newOverallBestTime
      );
    } else {
      stopArrivalsState.rejectNewBestTransitTime(stop, arrivalTime, trip, boardStop, boardTime);
    }
  }

  @Override
  public TransitArrival<T> previousTransit(int boardStopIndex) {
    return stopArrivalsState.previousTransit(boardStopIndex);
  }

  @Override
  public StopArrivals extractStopArrivals() {
    return new StopArrivalsAdaptor(bestTimes, stopArrivalsState);
  }

  private void transferToStop(int arrivalTimeTransit, int fromStop, RaptorTransfer transfer) {
    // Use the calculator to make sure the calculation is done correct for a normal
    // forward search and a reverse search.
    final int arrivalTime = calculator.plusDuration(
      arrivalTimeTransit,
      transfer.durationInSeconds()
    );

    if (exceedsTimeLimit(arrivalTime)) {
      return;
    }

    final int toStop = transfer.stop();

    if (newOverallBestTime(toStop, arrivalTime)) {
      stopArrivalsState.setNewBestTransferTime(fromStop, arrivalTime, transfer);
    } else {
      stopArrivalsState.rejectNewBestTransferTime(fromStop, arrivalTime, transfer);
    }
  }

  /* private methods */

  private boolean newOverallBestTime(int stop, int alightTime) {
    return bestTimes.updateNewBestTime(stop, alightTime);
  }

  private boolean newBestTransitArrivalTime(int stop, int alightTime) {
    return bestTimes.updateBestTransitArrivalTime(stop, alightTime);
  }

  private boolean exceedsTimeLimit(int time) {
    return calculator.exceedsTimeLimit(time);
  }
}
