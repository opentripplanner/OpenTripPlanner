package org.opentripplanner.raptor.rangeraptor.multicriteria.passthrough;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.rangeraptor.internalapi.PassThroughPointsService;

/**
 * IMPLEMENTATION DETAILS
 * <p>
 * We update the c2 value only if the current value is 1 less than the pass-through point
 * sequence number. This make sure pass-through points are visited in the right order.
 * The c2 value is equal to the last visited pass-through point sequence number. If zero
 * no pass-through point is visited yet.
 */
public class BitSetPassThroughPointsService implements PassThroughPointsService {

  private final List<BitSet> passThroughPoints;
  private final int expectedC2ValueAtDestination;
  private int currentPassThroughPointSeqNo = 0;

  private BitSetPassThroughPointsService(final List<BitSet> passThroughPoints) {
    this.passThroughPoints = passThroughPoints;
    this.expectedC2ValueAtDestination = passThroughPoints.size();
  }

  public static PassThroughPointsService of(List<RaptorViaLocation> locations) {
    return locations == null || locations.isEmpty()
      ? NOOP
      : locations
        .stream()
        .map(RaptorViaLocation::asBitSet)
        .collect(collectingAndThen(toList(), BitSetPassThroughPointsService::new));
  }

  @Override
  public boolean isPassThroughPoint(int stop) {
    for (int i = 0; i < passThroughPoints.size(); ++i) {
      if (passThroughPoints.get(i).get(stop)) {
        currentPassThroughPointSeqNo = i + 1;
        return true;
      }
    }
    // Make sure the c2 is NOT set if the stop is not a pass-through point
    this.currentPassThroughPointSeqNo = RaptorConstants.NOT_SET;
    return false;
  }

  @Override
  public void updateC2Value(int currentPathC2, IntConsumer update) {
    if (currentPassThroughPointSeqNo == currentPathC2 + 1) {
      update.accept(currentPassThroughPointSeqNo);
    }
  }

  @Override
  public DominanceFunction dominanceFunction() {
    return (left, right) -> left > right;
  }

  @Override
  public IntPredicate acceptC2AtDestination() {
    return c2 -> c2 == expectedC2ValueAtDestination;
  }
}
