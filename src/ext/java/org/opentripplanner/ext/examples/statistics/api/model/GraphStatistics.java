package org.opentripplanner.ext.examples.statistics.api.model;

import org.opentripplanner.routing.graph.GraphIndex;

/**
 * Simple POJO to define the returned 'key numbers'.
 */
public class GraphStatistics {
    private int stops;


    GraphStatistics(GraphIndex index) {
        this.stops = index.stopForId.size();
    }

    /**
     * The number of stops in the graph.
     */
    public int getStops() {
        return stops;
    }
}
