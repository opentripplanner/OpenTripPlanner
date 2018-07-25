package org.opentripplanner.routing.impl;

import java.util.Comparator;

import org.opentripplanner.routing.spt.GraphPath;

public class PathStartTimeComparator implements Comparator<GraphPath> {

    private static final PathStartTimeComparator instance = new PathStartTimeComparator();
    
    public static PathStartTimeComparator getInstance() {
        return instance;
    }
    
    private PathStartTimeComparator() {
    }
    
    /**
     * For arrive-before search results sort by departure time descending
     */
    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        return Long.compare(o2.getStartTime(), o1.getStartTime());
    }

}
