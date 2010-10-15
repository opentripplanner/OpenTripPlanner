package org.opentripplanner.routing.impl;

import java.util.Comparator;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.GraphPath;

public class PathComparator implements Comparator<GraphPath> {

    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        State state1 = o1.vertices.lastElement().state;
        State state2 = o2.vertices.lastElement().state;
        return (int) (state1.getTime() - state2.getTime());
    }

}
