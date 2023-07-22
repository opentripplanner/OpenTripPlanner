package org.opentripplanner.raptor.api.request;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * IMPLEMENTATION DETAILS
 * <p>
 * We update the c2 value only if the current value is 1 less than the pass-through point
 * sequence number. This make sure pass-through points are visited in the right order.
 * The c2 value is equal to the last visited pass-through point sequence number. If 0(zero)
 * no pass-through point is visited yet.
 */
public class BitSetPassThroughPoints implements PassThroughPoints {

  private final List<BitSet> passThroughPoints;
  private int currentPassThroughPointSeqNo = 0;

  private BitSetPassThroughPoints(final List<BitSet> passThroughPoints) {
    this.passThroughPoints = passThroughPoints;
  }

  public static PassThroughPoints create(final List<int[]> passThroughStops) {
    if (passThroughStops.isEmpty()) {
      return PassThroughPoints.NOOP;
    }

    return passThroughStops
      .stream()
      .map(pts -> {
        final BitSet tmpBitSet = new BitSet();
        Arrays.stream(pts).forEach(tmpBitSet::set);
        return tmpBitSet;
      })
      .collect(collectingAndThen(toList(), BitSetPassThroughPoints::new));
  }

  @Override
  public boolean isPassThroughPoint(int stop) {
    this.currentPassThroughPointSeqNo = 0;

    for (int i = 0; i < passThroughPoints.size(); ++i) {
      if (passThroughPoints.get(i).get(stop)) {
        currentPassThroughPointSeqNo = i + 1;
        return true;
      }
    }
    return false;
  }

  @Override
  public void updateC2Value(int currentC2, IntConsumer update) {
    if (currentPassThroughPointSeqNo == currentC2 + 1) {
      update.accept(currentPassThroughPointSeqNo);
    }
  }

  @Override
  public int size() {
    return passThroughPoints.size();
  }
}
