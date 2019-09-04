package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.spt.GraphPath;

import java.util.Comparator;

public class DurationComparator implements Comparator<GraphPath> {

    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        return o1.getDuration() - o2.getDuration();
    }

}
