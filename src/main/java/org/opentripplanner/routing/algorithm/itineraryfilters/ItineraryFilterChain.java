package org.opentripplanner.routing.algorithm.itineraryfilters;

import org.opentripplanner.routing.algorithm.ItineraryFilter;

import java.util.ArrayList;
import java.util.List;

public class ItineraryFilterChain {
    // We use a constant until we have a way to configure this dynamically
    public static final List<ItineraryFilter> FILTERS = new ArrayList<>();

    static {
        FILTERS.add(new FilterAwayLongWalkingTransit());
    }
}
