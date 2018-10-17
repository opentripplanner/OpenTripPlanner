package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.List;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graph.VertexComparatorFactory;

public class MortonVertexComparatorFactory implements VertexComparatorFactory, Serializable {
    private static final long serialVersionUID = -6904862616793682390L;

    public MortonVertexComparator getComparator(List<Vertex> domain) {
        return new MortonVertexComparator(domain);
    }

}