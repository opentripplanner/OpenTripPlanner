package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TripPatternMapper {

  /**
   * Convert all old TripPatterns into new ones, keeping a Map between the two.
   * Do this conversion up front (rather than lazily on demand) to ensure pattern IDs match
   * the sequence of patterns in source data.
   */
  static Map<org.opentripplanner.model.TripPattern, TripPattern> mapOldTripPatternToRaptorTripPattern(
      StopIndexForRaptor stopIndex,
      Collection<org.opentripplanner.model.TripPattern> oldTripPatterns
  ) {
    Map<org.opentripplanner.model.TripPattern, TripPattern> newTripPatternForOld;
    newTripPatternForOld = new HashMap<>();

    for (org.opentripplanner.model.TripPattern oldTripPattern : oldTripPatterns) {
      TripPattern newTripPattern = new TripPattern(
          // TripPatternForDate should never access the tripTimes inside the TripPattern,
          // so I've left them null.
          // No TripSchedules in the pattern itself; put them in the TripPatternForDate
          null,
          oldTripPattern.mode,
          stopIndex.listStopIndexesForStops(oldTripPattern.stopPattern.stops),
          oldTripPattern
      );
      newTripPatternForOld.put(oldTripPattern, newTripPattern);
    }
    return newTripPatternForOld;
  }
}
