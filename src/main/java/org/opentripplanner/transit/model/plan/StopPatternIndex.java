package org.opentripplanner.transit.model.plan;

import java.util.BitSet;
import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.RoutingTripPatternV2;

public class StopPatternIndex {

  private BitSet[] patternsByStopIndex;

  public StopPatternIndex(
    int nStop,
    Collection<RoutingTripPatternV2> patterns,
    @Nullable Deduplicator deduplicator
  ) {
    this.patternsByStopIndex = new BitSet[nStop];

    for (int i = 0; i < patternsByStopIndex.length; i++) {
      this.patternsByStopIndex[i] = new BitSet();
    }

    for (var pattern : patterns) {
      int pIndex = pattern.patternIndex();
      for (int stopPos = 0; stopPos < pattern.numberOfStopsInPattern(); ++stopPos) {
        this.patternsByStopIndex[pattern.stopIndex(stopPos)].set(pIndex);
      }
    }
    if (deduplicator != null) {
      for (int i = 0; i < patternsByStopIndex.length; i++) {
        this.patternsByStopIndex[i] = deduplicator.deduplicateBitSet(this.patternsByStopIndex[i]);
      }
    }
  }

  public BitSet activePatternsByStops(IntIterator stops) {
    BitSet activePatterns = new BitSet();
    while (stops.hasNext()) {
      activePatterns.or(patternsByStopIndex[stops.next()]);
    }
    return activePatterns;
  }
}
