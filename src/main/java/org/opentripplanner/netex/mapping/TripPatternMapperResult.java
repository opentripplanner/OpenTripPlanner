package org.opentripplanner.netex.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.netex.index.OrderedListMap;

/**
 * This mapper returnes two collections, so we need to use a simple wraper to be able to return the
 * result from the mapping method.
 */
class TripPatternMapperResult {

    /**
     * A map from trip/serviceJourney id to a ordered list of scheduled stop point ids.
     */
    final OrderedListMap<String, String> scheduledStopPointsIndex = new OrderedListMap<>();

    final Map<Trip, List<StopTime>> tripStopTimes = new HashMap<>();

    final List<TripPattern> tripPatterns = new ArrayList<>();

    /**
     * stopTimes by the timetabled-passing-time id
     */
    final Map<String, StopTime> stopTimeByNetexId = new HashMap<>();
}
