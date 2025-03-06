package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * This class is used to adjust the walk time - giving each path an unique generalized-cost. We want
 * the paths which do NOT visit the pass-through points to have a lower generalized-cost than the
 * "correct" paths. We do this by adding a small increasing cost to each paths ordered by the number
 * of stops visited. Then we add a bigger cost for each stop in the transfer which is a
 * pass-through-point.
 */
class WalkDurationForStopCombinations {

  private final int[] stopCost;
  private final Map<StopPair, Integer> transferCost = new HashMap<>();

  public WalkDurationForStopCombinations(int nStops) {
    stopCost = IntUtils.intArray(nStops, 0);
  }

  WalkDurationForStopCombinations withPassThroughPoints(
    Collection<RaptorViaLocation> viaLocations,
    int passThroughPointExtraCost
  ) {
    var passThroughStops = new BitSet();
    viaLocations.stream().map(RaptorViaLocation::asBitSet).forEach(passThroughStops::or);

    for (int i = 0; i < stopCost.length; i++) {
      if (passThroughStops.get(i)) {
        stopCost[i] += passThroughPointExtraCost;
      }
    }
    return this;
  }

  /**
   * Use this method to add a small cost({@code costInWalkSec}) to a transfer. Give the path that
   * visit the least number of stops the lowest value. In addition, the
   * {@link #withPassThroughPoints(Collection, int)} method can be used to add a extra cost to all
   * transfers containing a pass-through-point.
   */
  WalkDurationForStopCombinations addTxCost(int fromStop, int toStop, int costInWalkSec) {
    StopPair tx = new StopPair(fromStop, toStop);
    transferCost.put(tx, costTxOnly(tx) + costInWalkSec);
    return this;
  }

  int walkDuration(int fromStop, int toStop) {
    return costTxOnly(new StopPair(fromStop, toStop)) + stopCost[fromStop] + stopCost[toStop];
  }

  int costTxOnly(StopPair tx) {
    return transferCost.getOrDefault(tx, 0);
  }
}
