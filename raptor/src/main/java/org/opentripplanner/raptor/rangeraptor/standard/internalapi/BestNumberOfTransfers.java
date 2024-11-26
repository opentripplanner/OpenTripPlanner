package org.opentripplanner.raptor.rangeraptor.standard.internalapi;

import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;

/**
 * This interface is part of calculating heuristics for transfers.
 */
public interface BestNumberOfTransfers {
  /**
   * If a stop is not reached the {@link #calculateMinNumberOfTransfers(int)} should return this
   * value. The value is a very high number.
   */
  default int unreachedMinNumberOfTransfers() {
    return RaptorConstants.N_TRANSFERS_UNREACHED;
  }

  /**
   * Return the minimum number for transfers used to reach the given stop.
   * <p/>
   * This method is called after the search is complete, not before.
   * <p/>
   * The result is used to calculate heuristics, so the calculated value should be less than or
   * equal to the "real value". The value should be at most: -1 for a stop which is directly
   * reachable via a street access/egress 0 for a stop which can be reached via a single transit leg
   * 1 for a stop which can be reached via two separate transit legs
   */
  int calculateMinNumberOfTransfers(int stopIndex);

  /**
   * Return the best-overall number of transfers for each stop arrival.
   */
  SingleCriteriaStopArrivals extractBestNumberOfTransfers();
}
