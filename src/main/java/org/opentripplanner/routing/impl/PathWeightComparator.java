package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.spt.GraphPath;

import java.util.Comparator;

public class PathWeightComparator implements Comparator<GraphPath> {

    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        return (int) (o1.getWeight() - o2.getWeight());
    }

}
