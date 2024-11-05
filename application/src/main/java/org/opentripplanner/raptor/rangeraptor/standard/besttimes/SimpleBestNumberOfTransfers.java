package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.BestNumberOfTransfers;
import org.opentripplanner.raptor.rangeraptor.support.IntArraySingleCriteriaArrivals;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * The responsibility for this class is to keep track of the best (minimun) number of transfers for
 * all stops reached.
 */
public class SimpleBestNumberOfTransfers implements BestNumberOfTransfers {

  private final int[] bestNumOfTransfers;
  private int round;

  public SimpleBestNumberOfTransfers(int nStops, WorkerLifeCycle lifeCycle) {
    this.bestNumOfTransfers = IntUtils.intArray(nStops, unreachedMinNumberOfTransfers());

    lifeCycle.onPrepareForNextRound(r -> this.round = r);
  }

  @Override
  public int calculateMinNumberOfTransfers(int stop) {
    return bestNumOfTransfers[stop];
  }

  /**
   * Call this method to notify that the given stop is reached in the current round of Raptor.
   */
  void arriveAtStop(int stop) {
    final int numOfTransfers = round - 1;
    if (numOfTransfers < bestNumOfTransfers[stop]) {
      bestNumOfTransfers[stop] = numOfTransfers;
    }
  }

  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    return new IntArraySingleCriteriaArrivals(unreachedMinNumberOfTransfers(), bestNumOfTransfers);
  }
}
