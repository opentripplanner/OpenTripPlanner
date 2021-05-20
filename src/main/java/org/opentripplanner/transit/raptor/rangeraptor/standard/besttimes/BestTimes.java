package org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes;

import static org.opentripplanner.transit.raptor.util.IntUtils.intArray;

import java.util.BitSet;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.util.BitSetIterator;


/**
 * This class is responsible for keeping track of the overall best times and
 * the best transit times. In addition it keeps track of times updated
 * in the current round and previous round. It is optimized for performance,
 * all information here is also in the state, but this class keeps things in
 * the fastest possible data structure.
 * <p/>
 * We keep track of the best over all times to be able to drop a new arrivals
 * exceeding the time already found by another branch.
 * <p/>
 * We need to keep track of the transit times, not only the overall bet times,
 * to find all the best transfers. When arriving at a stop by transit, we need
 * to find all transfers to other stops, event if there is another transfer
 * arrival with a better arrival time. The reason is that after transfer to the
 * next stop, the new arrival may become the best time at that stop. Two transfers
 * are after each other is not legal.
 */
public final class BestTimes {

    /** The best times to reach a stop, across rounds and iterations. */
    private final int[] times;

    /** The best transit times to reach a stop, across rounds and iterations. */
    private final int[] transitTimes;

    /** Stops touched by transit or transfers in the CURRENT round. */
    private BitSet reachedCurrentRound;
    private final BitSet transitReachedCurrentRound;

    /** Stops touched by transit or transfers in LAST round. */
    private BitSet reachedLastRound;

    private final TransitCalculator<?> calculator;


    public BestTimes(int nStops, TransitCalculator<?> calculator, WorkerLifeCycle lifeCycle) {
        this.calculator = calculator;
        this.times = intArray(nStops, calculator.unreachedTime());
        this.reachedCurrentRound = new BitSet(nStops);
        this.reachedLastRound = new BitSet(nStops);

        this.transitTimes = intArray(nStops, calculator.unreachedTime());
        this.transitReachedCurrentRound = new BitSet(nStops);

        // Attach to Worker life cycle
        lifeCycle.onSetupIteration((ignore) -> setupIteration());
        lifeCycle.onPrepareForNextRound(round -> prepareForNextRound());
    }

    public final int time(int stop) {
        return times[stop];
    }

    public final int transitTime(int stop) {
        return transitTimes[stop];
    }

    /**
     * Clear all reached flags before we start a new iteration.
     * This is important so stops visited in the previous
     * iteration in the last round does not "overflow" into
     * the next iteration.
     */
    private void setupIteration() {
        // clear all touched stops to avoid constant reëxploration
        reachedCurrentRound.clear();
        transitReachedCurrentRound.clear();
    }

    /**
     * @return true if at least one stop arrival was reached last round (best overall).
     */
    public final boolean isCurrentRoundUpdated() {
        return !reachedCurrentRound.isEmpty();
    }

    /**
     * Prepare this class for the next round updating reached flags.
     */
    private void prepareForNextRound() {
        swapReachedCurrentAndLastRound();
        reachedCurrentRound.clear();
        transitReachedCurrentRound.clear();
    }

    /**
     * @return an iterator for all stops reached (overall best) in the last round.
     */
    public final BitSetIterator stopsReachedLastRound() {
        return new BitSetIterator(reachedLastRound);
    }

    /**
     * @return an iterator of all stops reached by transit in the current round.
     */
    public final BitSetIterator transitStopsReachedCurrentRound() {
        return new BitSetIterator(transitReachedCurrentRound);
    }

    /**
     * @return true if the given stop was reached by transit in the current round.
     */
    final boolean isStopReachedByTransitCurrentRound(int stop) {
        return transitReachedCurrentRound.get(stop);
    }

    /**
     * @return true if the given stop was reached in the previous/last round.
     */
    public final boolean isStopReachedLastRound(int stop) {
        return reachedLastRound.get(stop);
    }


    /**
     * @return return true if stop is reached.
     */
    public final boolean isStopReached(int stop) {
        return time(stop) != calculator.unreachedTime();
    }

    /**
     * Set the initial access time at the given stop, but only if the new access
     * is better than a stop arrival from previous rounds.
     * <p/>
     * This is equivalent to calling {@link #updateNewBestTime(int, int)}
     */
    public final void setAccessStopTime(final int stop, final int time, final boolean arrivedViaTransit) {
        updateNewBestTime(stop, time);
        if (arrivedViaTransit) {
           transitUpdateNewBestTime(stop, time);
        }
    }

    /**
     * @return true iff new best time is updated
     */
    public final boolean transitUpdateNewBestTime(int stop, int time) {
        if(isBestTransitTime(stop, time)) {
            setTransitTime(stop, time);
            return true;
        }
        return false;
    }

    /**
     * @return true iff new best time is updated
     */
    public final boolean updateNewBestTime(int stop, int time) {
        if(isBestTime(stop, time)) {
            setTime(stop, time);
            return true;
        }
        return false;
    }

    public final int size() {
        return times.length;
    }

    @Override
    public String toString() {
        final int unreachedTime = calculator.unreachedTime();
        return ToStringBuilder.of(BestTimes.class)
            .addIntArraySize("times", times, unreachedTime)
            .addIntArraySize("transitTimes", transitTimes, unreachedTime)
            .addNum("reachedCurrentRound", reachedCurrentRound.size())
            .addBitSetSize("transitReachedCurrentRound", transitReachedCurrentRound)
            .addBitSetSize("reachedLastRound", reachedLastRound)
            .toString();
    }

    /* private methods */

    private void setTime(final int stop, final int time) {
        times[stop] = time;
        reachedCurrentRound.set(stop);
    }

    private boolean isBestTime(int stop, int time) {
        return calculator.isBest(time, times[stop]);
    }

    private boolean isBestTransitTime(int stop, int time) {
        return calculator.isBest(time, transitTimes[stop]);
    }

    private void setTransitTime(int stop, int time) {
        transitTimes[stop] = time;
        transitReachedCurrentRound.set(stop);
    }

    private void swapReachedCurrentAndLastRound() {
        BitSet tmp = reachedLastRound;
        reachedLastRound = reachedCurrentRound;
        reachedCurrentRound = tmp;
    }
}
