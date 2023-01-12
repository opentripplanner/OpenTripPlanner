package org.opentripplanner.raptor.heuristic;

import static org.opentripplanner.framework.lang.IntUtils.intArray;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.standard.StdWorkerState;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;
import org.opentripplanner.raptor.util.BitSetIterator;

/**
 * The state for a {@link HeuristicRoutingStrategy}, which considers both time and cost criteria.
 */
public class TimeAndCostHeuristicState<T extends RaptorTripSchedule> implements StdWorkerState<T> {

  public static final int UNREACHED = 999_999_999;
  private final int[] egressStops;

  private final int[] bestNumOfTransfers;

  /** The best times to reach a stop, across rounds and iterations. */
  private final int[] times;

  /**
   * The best "on-board" arrival times to reach a stop, across rounds and iterations. It includes
   * both transit arrivals and access-on-board arrivals.
   */
  private final int[] transitArrivalTimes;

  private final int[] costs;

  private final int[] transitArrivalCosts;

  private final BitSet reachedByTransitCurrentRound;

  private final RoundProvider roundProvider;

  /** Stops touched in the CURRENT round. */
  private BitSet reachedCurrentRound;
  /** Stops touched by in LAST round. */
  private BitSet reachedLastRound;

  public TimeAndCostHeuristicState(
    int[] egressStops,
    int nStops,
    RoundProvider roundProvider,
    WorkerLifeCycle lifeCycle
  ) {
    this.egressStops = egressStops;
    this.bestNumOfTransfers = intArray(nStops, UNREACHED);
    this.times = intArray(nStops, UNREACHED);
    this.transitArrivalTimes = intArray(nStops, UNREACHED);
    this.costs = intArray(nStops, UNREACHED);
    this.transitArrivalCosts = intArray(nStops, UNREACHED);
    this.reachedByTransitCurrentRound = new BitSet(nStops);
    this.reachedCurrentRound = new BitSet(nStops);
    this.reachedLastRound = new BitSet(nStops);
    this.roundProvider = roundProvider;

    // Attach to Worker life cycle
    lifeCycle.onSetupIteration(ignore -> setupIteration());
    lifeCycle.onPrepareForNextRound(round -> prepareForNextRound());
  }

  @Override
  public boolean isNewRoundAvailable() {
    return !reachedCurrentRound.isEmpty();
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    // This is fast enough, we could use a BitSet for egressStops, but it takes up more
    // memory and the performance is the same.
    for (final int egressStop : egressStops) {
      if (reachedByTransitCurrentRound.get(egressStop)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public IntIterator stopsTouchedPreviousRound() {
    return new BitSetIterator(reachedLastRound);
  }

  @Override
  public IntIterator stopsTouchedByTransitCurrentRound() {
    return new BitSetIterator(reachedByTransitCurrentRound);
  }

  @Override
  public boolean isStopReachedInPreviousRound(int stopIndex) {
    return reachedLastRound.get(stopIndex);
  }

  public int bestTimePreviousRound(int stopIndex) {
    return times[stopIndex];
  }

  @Override
  public void transitToStop(int alightStop, int alightTime, int boardStop, int boardTime, T trip) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public TransitArrival<T> previousTransit(int boardStopIndex) {
    throw new IllegalStateException("Not implemented");
  }

  public int bestCostPreviousRound(int stopIndex) {
    return costs[stopIndex];
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    updateNewBestTimeCostAndRound(
      accessPath.stop(),
      accessPath.durationInSeconds(),
      accessPath.generalizedCost(),
      accessPath.stopReachedOnBoard()
    );
  }

  @Override
  public void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
    final int prevDuration = bestTimePreviousRound(fromStop);
    final int prevCost = bestCostPreviousRound(fromStop);

    while (transfers.hasNext()) {
      RaptorTransfer it = transfers.next();
      updateNewBestTimeCostAndRound(
        it.stop(),
        it.durationInSeconds() + prevDuration,
        it.generalizedCost() + prevCost,
        false
      );
    }
  }

  void updateNewBestTimeCostAndRound(int stop, int time, int cost, boolean isTransit) {
    if (!isTransit || updateBestTransitArrivalTime(stop, time)) {
      updateBestTime(stop, time);
    }
    if (!isTransit || updateBestTransitArrivalCost(stop, cost)) {
      updateBestCost(stop, cost);
    }
    updateBestRound(stop);
  }

  @Override
  public Collection<Path<T>> extractPaths() {
    return List.of();
  }

  @Override
  public StopArrivals extractStopArrivals() {
    return null;
  }

  public Heuristics heuristics() {
    return new TimeAndCostHeuristicsAdapter(bestNumOfTransfers, times, costs, egressStops);
  }

  /**
   * Clear all reached flags before we start a new iteration. This is important so stops visited in
   * the previous iteration in the last round does not "overflow" into the next iteration.
   */
  private void setupIteration() {
    // clear all touched stops to avoid constant reëxploration
    reachedCurrentRound.clear();
    reachedByTransitCurrentRound.clear();
  }

  private void swapReachedCurrentAndLastRound() {
    BitSet tmp = reachedLastRound;
    reachedLastRound = reachedCurrentRound;
    reachedCurrentRound = tmp;
  }

  private boolean updateBestTime(int stop, int time) {
    if (time < times[stop]) {
      times[stop] = time;
      reachedCurrentRound.set(stop);
      return true;
    }
    return false;
  }

  private boolean updateBestTransitArrivalTime(int stop, int time) {
    if (time < transitArrivalTimes[stop]) {
      transitArrivalTimes[stop] = time;
      reachedByTransitCurrentRound.set(stop);
      return true;
    }
    return false;
  }

  private boolean updateBestCost(int stop, int cost) {
    if (cost < costs[stop]) {
      costs[stop] = cost;
      reachedCurrentRound.set(stop);
      return true;
    }
    return false;
  }

  private boolean updateBestTransitArrivalCost(int stop, int cost) {
    if (cost < transitArrivalCosts[stop]) {
      transitArrivalCosts[stop] = cost;
      reachedByTransitCurrentRound.set(stop);
      return true;
    }
    return false;
  }

  void updateBestRound(int stop) {
    final int numOfTransfers = roundProvider.round() - 1;
    if (numOfTransfers < bestNumOfTransfers[stop]) {
      bestNumOfTransfers[stop] = numOfTransfers;
    }
  }

  /**
   * Prepare this class for the next round updating reached flags.
   */
  private void prepareForNextRound() {
    swapReachedCurrentAndLastRound();
    reachedCurrentRound.clear();
    reachedByTransitCurrentRound.clear();
  }
}
