package org.opentripplanner.routing.impl;

import java.util.Comparator;

import org.opentripplanner.routing.spt.GraphPath;

public class PathEndTimeComparator implements Comparator<GraphPath> {

    private static final PathEndTimeComparator instance = new PathEndTimeComparator();

    public static PathEndTimeComparator getInstance() {
        return instance;
    }
    
    private PathEndTimeComparator() {
    }

    /**
     * For depart-after search results sort by arrival time ascending
     */
    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        return Long.compare(o1.getEndTime(), o2.getEndTime());
    }

}
