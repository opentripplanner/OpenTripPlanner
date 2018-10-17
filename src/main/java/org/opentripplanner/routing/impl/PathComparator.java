package org.opentripplanner.routing.impl;

import java.util.Comparator;

import org.opentripplanner.routing.spt.GraphPath;

public class PathComparator implements Comparator<GraphPath> {

    boolean compareStartTimes;
    
    public PathComparator(boolean compareStartTimes) {
        this.compareStartTimes = compareStartTimes;
    }
    
    /**
     * For depart-after search results sort by arrival time ascending
     * For arrive-before search results sort by departure time descending
     */
    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        if (compareStartTimes) {
            return (int) (o2.getStartTime() - o1.getStartTime());
        } else {
            return (int) (o1.getEndTime() - o2.getEndTime());
        }
    }

}
