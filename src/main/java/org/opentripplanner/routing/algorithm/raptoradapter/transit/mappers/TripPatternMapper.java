package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.transit.model.network.TripPattern;

public class TripPatternMapper {

  private final Map<TripPattern, TripPatternWithRaptorStopIndexes> newTripPatternForOld = new HashMap<>();

  /**
   * Convert all old TripPatterns into new ones, keeping a Map between the two. Do this conversion
   * up front (rather than lazily on demand) to ensure pattern IDs match the sequence of patterns in
   * source data.
   */
  Map<TripPattern, TripPatternWithRaptorStopIndexes> mapOldTripPatternToRaptorTripPattern(
    Collection<TripPattern> oldTripPatterns
  ) {
    for (TripPattern oldTripPattern : oldTripPatterns) {
      if (newTripPatternForOld.containsKey(oldTripPattern)) {
        continue;
      }
      TripPatternWithRaptorStopIndexes newTripPattern = new TripPatternWithRaptorStopIndexes(
        oldTripPattern
      );
      newTripPatternForOld.put(oldTripPattern, newTripPattern);
    }
    return newTripPatternForOld;
  }
}
