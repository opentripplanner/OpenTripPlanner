package org.opentripplanner.ext.examples.statistics.api.model;

import org.opentripplanner.routing.RoutingService;

/**
 * Simple POJO to define the returned 'key numbers'.
 */
public class GraphStatistics {
    private int stops;


    GraphStatistics(RoutingService index) {
        this.stops = index.getAllStops().size();
    }

    /**
     * The number of stops in the graph.
     */
    public int getStops() {
        return stops;
    }
}
