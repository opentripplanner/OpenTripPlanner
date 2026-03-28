package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.Iterator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.ArrivedAtDestinationCheck;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.BestNumberOfTransfers;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.StopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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

  private static final boolean EARLY_PRUNING_ENABLED =
    !"false".equals(System.getProperty("otp.raptor.earlyPruning", "true"));

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
   * Used to extract the best number-of-transfers as part of the result.
   */
  private final BestNumberOfTransfers bestNumberOfTransfers;

  /**
   * The list of egress stops, can be used to terminate the search when the stops are reached.
   */
  private final ArrivedAtDestinationCheck arrivedAtDestinationCheck;

  /**
   * The calculator is used to calculate transit related times/events like access arrival time.
   */
  private final RaptorTransitCalculator<T> calculator;

  /**
   * Early Pruning: egress stop indices and their minimum egress durations. For each egress stop,
   * we track the minimum duration of all egress paths from that stop. The best destination arrival
   * time = min(arrivalTimeAtEgressStop + minEgressDuration).
   * See: Rohovyi et al., "Early Pruning for Public Transport Routing", 2026.
   */
  private final int[] egressStopIndices;
  private final int[] egressMinDurations;

  /**
   * Early Pruning: best known destination arrival time per round (across all iterations).
   * Each round only prunes against its own best, so a fast multi-transfer path from a previous
   * iteration does not block a slower few-transfer path in the current iteration.
   * This ensures correctness for Range Raptor's [departureTime, arrivalTime, numTransfers] Pareto.
   */
  private final int[] bestDestArrivalByRound;

  /**
   * Early Pruning: best known destination arrival time within the current RAPTOR iteration.
   * Within a single iteration (fixed departure time), rounds proceed sequentially, so this
   * value only reflects earlier rounds. It is reset at the start of each iteration.
   */
  private int bestDestCurrentIteration;

  /** Current round, updated via lifecycle. */
  private int currentRound;

  /**
   * create a BestTimes Range Raptor State for given context.
   */
  public StdRangeRaptorWorkerState(
    RaptorTransitCalculator<T> calculator,
    BestTimes bestTimes,
    StopArrivalsState<T> stopArrivalsState,
    BestNumberOfTransfers bestNumberOfTransfers,
    ArrivedAtDestinationCheck arrivedAtDestinationCheck,
    int[] egressStopIndices,
    int[] egressMinDurations,
    int nRounds,
    WorkerLifeCycle lifeCycle
  ) {
    this.calculator = calculator;
    this.bestTimes = bestTimes;
    this.stopArrivalsState = stopArrivalsState;
    this.bestNumberOfTransfers = bestNumberOfTransfers;
    this.arrivedAtDestinationCheck = arrivedAtDestinationCheck;
    this.egressStopIndices = egressStopIndices;
    this.egressMinDurations = egressMinDurations;
    this.bestDestArrivalByRound = new int[nRounds + 1];
    java.util.Arrays.fill(this.bestDestArrivalByRound, calculator.unreachedTime());
    this.bestDestCurrentIteration = calculator.unreachedTime();
    this.currentRound = 0;
    lifeCycle.onPrepareForNextRound(round -> this.currentRound = round);
    lifeCycle.onSetupIteration(ignore -> this.bestDestCurrentIteration = calculator.unreachedTime());
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
  public IntIterator stopsTouchedByTransitCurrentRound() {
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
   * Transfers are expected to be sorted by duration (non-decreasing). This enables Early Pruning:
   * once a transfer exceeds the time limit, all subsequent (longer) transfers will too, so the
   * loop can terminate early. See: Rohovyi et al., "Early Pruning for Public Transport Routing".
   */
  @Override
  public void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
    int arrivalTimeTransit = bestTimes.transitArrivalTime(fromStop);
    if (EARLY_PRUNING_ENABLED) {
      while (transfers.hasNext()) {
        if (transferToStopWithEarlyPruning(arrivalTimeTransit, fromStop, transfers.next())) {
          break;
        }
      }
    } else {
      while (transfers.hasNext()) {
        transferToStopWithEarlyPruning(arrivalTimeTransit, fromStop, transfers.next());
      }
    }
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

  /**
   * @return true if the time limit was exceeded, signaling that Early Pruning should terminate
   *     the transfer loop (all subsequent sorted transfers will also exceed the limit).
   */
  private boolean transferToStopWithEarlyPruning(
    int arrivalTimeTransit,
    int fromStop,
    RaptorTransfer transfer
  ) {
    // Use the calculator to make sure the calculation is done correct for a normal
    // forward search and a reverse search.
    final int arrivalTime = calculator.plusDuration(
      arrivalTimeTransit,
      transfer.durationInSeconds()
    );

    if (exceedsTimeLimit(arrivalTime)) {
      return true;
    }

    // Early Pruning: use the tighter of two valid bounds:
    // 1. bestDestArrivalByRound[currentRound] — per-round best across all Range Raptor iterations
    // 2. bestDestCurrentIteration — best within current iteration (from earlier rounds)
    // Both are valid: (1) ensures cross-iteration correctness for Pareto [dept, arr, transfers],
    // (2) provides within-iteration pruning as in single-departure RAPTOR (paper's EP).
    // See: Rohovyi et al., "Early Pruning for Public Transport Routing", 2026.
    if (EARLY_PRUNING_ENABLED) {
      int epBound = bestDestCurrentIteration;
      if (
        currentRound < bestDestArrivalByRound.length &&
        calculator.isBefore(bestDestArrivalByRound[currentRound], epBound)
      ) {
        epBound = bestDestArrivalByRound[currentRound];
      }
      if (!calculator.isBefore(arrivalTime, epBound)) {
        return true;
      }
    }

    final int toStop = transfer.stop();

    if (newOverallBestTime(toStop, arrivalTime)) {
      stopArrivalsState.setNewBestTransferTime(fromStop, arrivalTime, transfer);
    } else {
      stopArrivalsState.rejectNewBestTransferTime(fromStop, arrivalTime, transfer);
    }
    return false;
  }

  @Override
  public RaptorRouterResult<T> results() {
    return new StdRaptorRouterResult<>(
      bestTimes,
      stopArrivalsState::extractPaths,
      bestNumberOfTransfers::extractBestNumberOfTransfers
    );
  }

  /* private methods */

  private boolean newOverallBestTime(int stop, int alightTime) {
    boolean updated = bestTimes.updateNewBestTime(stop, alightTime);
    if (updated) {
      // Early Pruning: update destination arrival times if this is an egress stop.
      for (int i = 0; i < egressStopIndices.length; i++) {
        if (egressStopIndices[i] == stop) {
          int destArrival = calculator.plusDuration(alightTime, egressMinDurations[i]);
          // Update per-round bound (for cross-iteration correctness)
          if (
            currentRound < bestDestArrivalByRound.length &&
            calculator.isBefore(destArrival, bestDestArrivalByRound[currentRound])
          ) {
            bestDestArrivalByRound[currentRound] = destArrival;
          }
          // Update within-iteration bound (for within-iteration pruning)
          if (calculator.isBefore(destArrival, bestDestCurrentIteration)) {
            bestDestCurrentIteration = destArrival;
          }
          break;
        }
      }
    }
    return updated;
  }

  private boolean newBestTransitArrivalTime(int stop, int alightTime) {
    return bestTimes.updateBestTransitArrivalTime(stop, alightTime);
  }

  private boolean exceedsTimeLimit(int time) {
    return calculator.exceedsTimeLimit(time);
  }
}
