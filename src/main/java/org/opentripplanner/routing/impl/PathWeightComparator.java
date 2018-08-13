package org.opentripplanner.routing.impl;

import java.util.Comparator;

import org.opentripplanner.routing.spt.GraphPath;

public class PathWeightComparator implements Comparator<GraphPath> {

    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        return (int) (o1.getWeight() - o2.getWeight());
    }

}
