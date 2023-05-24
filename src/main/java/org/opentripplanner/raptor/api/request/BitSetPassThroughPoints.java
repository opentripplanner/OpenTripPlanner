package org.opentripplanner.raptor.api.request;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class BitSetPassThroughPoints implements PassThroughPoints {

  private final List<BitSet> passThroughPoints;

  private BitSetPassThroughPoints(final List<BitSet> passThroughPoints) {
    this.passThroughPoints = passThroughPoints;
  }

  public static PassThroughPoints create(final List<int[]> passThroughStops) {
    if (passThroughStops.isEmpty()) {
      return PassThroughPoints.NO_PASS_THROUGH_POINTS;
    }

    return passThroughStops
      .stream()
      .map(pts -> {
        final BitSet tmpBitSet = new BitSet();
        Arrays.stream(pts).forEach(si -> tmpBitSet.set(si));
        return tmpBitSet;
      })
      .collect(collectingAndThen(toList(), vps -> new BitSetPassThroughPoints(vps)));
  }

  @Override
  public boolean isPassThroughPoint(final int passThroughIndex, final int stop) {
    if (passThroughIndex >= passThroughPoints.size()) {
      return false;
    }
    return passThroughPoints.get(passThroughIndex).get(stop);
  }

  @Override
  public int size() {
    return passThroughPoints.size();
  }
}
