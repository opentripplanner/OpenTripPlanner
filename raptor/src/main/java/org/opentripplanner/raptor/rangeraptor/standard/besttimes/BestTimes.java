package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import static org.opentripplanner.utils.lang.IntUtils.intArray;

import java.util.BitSet;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.support.IntArraySingleCriteriaArrivals;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class is responsible for keeping track of the overall best times and the best "on-board"
 * times. In addition, it keeps track of times updated in the current round and previous round. It
 * is optimized for performance, all information here is also in the state, but this class keeps
 * things in the fastest possible data structure.
 * <p/>
 * We keep track of the best over all times to be able to drop a new arrivals exceeding the time
 * already found by another branch.
 * <p/>
 * We need to keep track of the arrive "on-board" times(transit and flex-on-board arrivals), not
 * only the overall bet times, to find all the best transfers. When arriving at a stop on-board, we
 * need to find all transfers to other stops, event if there is another transfer arrival with a
 * better arrival time. The reason is that after transfer to the next stop, the new arrival may
 * become the best time at that stop. Two transfers that are after each other are not allowed.
 */
public final class BestTimes {

  /** The best times to reach a stop, across rounds and iterations. */
  private final int[] times;

  /**
   * The best "on-board" arrival times to reach a stop, across rounds and iterations. It includes
   * both transit arrivals and access-on-board arrivals.
   */
  private final int[] transitArrivalTimes;
  private final BitSet reachedByTransitCurrentRound;
  private final TransitCalculator<?> calculator;
  /** Stops touched in the CURRENT round. */
  private BitSet reachedCurrentRound;
  /** Stops touched by in LAST round. */
  private BitSet reachedLastRound;

  public BestTimes(int nStops, TransitCalculator<?> calculator, WorkerLifeCycle lifeCycle) {
    this.calculator = calculator;
    this.times = intArray(nStops, calculator.unreachedTime());
    this.reachedCurrentRound = new BitSet(nStops);
    this.reachedLastRound = new BitSet(nStops);

    this.transitArrivalTimes = intArray(nStops, calculator.unreachedTime());
    this.reachedByTransitCurrentRound = new BitSet(nStops);

    // Attach to Worker life cycle
    lifeCycle.onSetupIteration(ignore -> setupIteration());
    lifeCycle.onPrepareForNextRound(round -> prepareForNextRound());
  }

  public int time(int stop) {
    return times[stop];
  }

  public int transitArrivalTime(int stop) {
    return transitArrivalTimes[stop];
  }

  /**
   * @return true if at least one stop arrival was reached last round (best overall).
   */
  public boolean isCurrentRoundUpdated() {
    return !reachedCurrentRound.isEmpty();
  }

  /**
   * @return an iterator for all stops reached (overall best) in the last round.
   */
  public BitSetIterator stopsReachedLastRound() {
    return new BitSetIterator(reachedLastRound);
  }

  /**
   * @return an iterator of all stops reached on-board in the current round.
   */
  public BitSetIterator reachedByTransitCurrentRound() {
    return new BitSetIterator(reachedByTransitCurrentRound);
  }

  /**
   * @return true if the given stop was reached in the previous/last round.
   */
  public boolean isStopReachedLastRound(int stop) {
    return reachedLastRound.get(stop);
  }

  /**
   * @return return true if stop is reached.
   */
  public boolean isStopReached(int stop) {
    return time(stop) != calculator.unreachedTime();
  }

  /**
   * @return return true if stop is reached.
   */
  public boolean isStopReachedByTransit(int stop) {
    return transitArrivalTime(stop) != calculator.unreachedTime();
  }

  /**
   * @return true iff new best time is updated
   */
  public boolean updateBestTransitArrivalTime(int stop, int time) {
    if (isBestTransitArrivalTime(stop, time)) {
      setBestTransitTime(stop, time);
      return true;
    }
    return false;
  }

  /**
   * @return true iff new best time is updated
   */
  public boolean updateNewBestTime(int stop, int time) {
    if (isBestTime(stop, time)) {
      setTime(stop, time);
      return true;
    }
    return false;
  }

  public int size() {
    return times.length;
  }

  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    return new IntArraySingleCriteriaArrivals(calculator.unreachedTime(), times);
  }

  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    return new IntArraySingleCriteriaArrivals(calculator.unreachedTime(), transitArrivalTimes);
  }

  @Override
  public String toString() {
    final int unreachedTime = calculator.unreachedTime();
    return ToStringBuilder.of(BestTimes.class)
      .addIntArraySize("times", times, unreachedTime)
      .addIntArraySize("transitArrivalTimes", transitArrivalTimes, unreachedTime)
      .addBitSetSize("reachedCurrentRound", reachedCurrentRound)
      .addBitSetSize("reachedByTransitCurrentRound", reachedByTransitCurrentRound)
      .addBitSetSize("reachedLastRound", reachedLastRound)
      .toString();
  }

  /**
   * @return true if the given stop was reached by on-board in the current round.
   */
  boolean isStopReachedOnBoardInCurrentRound(int stop) {
    return reachedByTransitCurrentRound.get(stop);
  }

  /**
   * @return true if the given stop was reached by transfer or on-board in the current round.
   */
  boolean isStopReachedInCurrentRound(int stop) {
    return reachedCurrentRound.get(stop);
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

  /**
   * Prepare this class for the next round updating reached flags.
   */
  private void prepareForNextRound() {
    swapReachedCurrentAndLastRound();
    reachedCurrentRound.clear();
    reachedByTransitCurrentRound.clear();
  }

  /* private methods */

  private void setTime(final int stop, final int time) {
    times[stop] = time;
    reachedCurrentRound.set(stop);
  }

  private boolean isBestTime(int stop, int time) {
    return calculator.isBefore(time, times[stop]);
  }

  private boolean isBestTransitArrivalTime(int stop, int time) {
    return calculator.isBefore(time, transitArrivalTimes[stop]);
  }

  private void setBestTransitTime(int stop, int time) {
    transitArrivalTimes[stop] = time;
    reachedByTransitCurrentRound.set(stop);
  }

  private void swapReachedCurrentAndLastRound() {
    BitSet tmp = reachedLastRound;
    reachedLastRound = reachedCurrentRound;
    reachedCurrentRound = tmp;
  }
}
