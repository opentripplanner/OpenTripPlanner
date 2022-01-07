package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;

public class TripPatternMapper {

    /**
     * Convert all old TripPatterns into new ones, keeping a Map between the two. Do this conversion
     * up front (rather than lazily on demand) to ensure pattern IDs match the sequence of patterns
     * in source data.
     */
    static Map<TripPattern, TripPatternWithRaptorStopIndexes> mapOldTripPatternToRaptorTripPattern(
            StopIndexForRaptor stopIndex, Collection<TripPattern> oldTripPatterns
    ) {
        Map<TripPattern, TripPatternWithRaptorStopIndexes> newTripPatternForOld;
        newTripPatternForOld = new HashMap<>();

        for (TripPattern oldTripPattern : oldTripPatterns) {
            TripPatternWithRaptorStopIndexes newTripPattern = new TripPatternWithRaptorStopIndexes(
                    oldTripPattern,
                    stopIndex.listStopIndexesForStops(oldTripPattern.getStops())
            );
            newTripPatternForOld.put(oldTripPattern, newTripPattern);
        }
        return newTripPatternForOld;
    }
}
