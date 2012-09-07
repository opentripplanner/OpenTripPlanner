package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class RegionData implements Serializable {
    private static final long serialVersionUID = 4835563169492792969L;

    public double[][] minWalk;

    public int[][] minTime;

    public int[] regionForVertex;

    public RegionData(int[] regionsForVertex) {
        this.regionForVertex = regionsForVertex;
    }

    public List<Integer> getRegionsForVertex(Vertex vertex) {
        return getRegionsForVertex(vertex, new HashSet<Vertex>());
    }

    public List<Integer> getRegionsForVertex(Vertex vertex, HashSet<Vertex> seen) {
        if (seen.contains(vertex)) {
            return Collections.emptyList();
        }
        seen.add(vertex);
        int index = regionForVertex[vertex.getIndex()];
        if (index >= 0) {
            return Arrays.asList(index);
        }
        ArrayList<Integer> regions = new ArrayList<Integer>();
        for (Edge e : vertex.getOutgoing()) {
            Vertex tov = e.getToVertex();
            for (int region : getRegionsForVertex(tov, seen)) {
                if (regions.contains(region))
                    continue;
                regions.add(region);
            }
        }
        return regions;
    }

    public int getRegionForVertex(Vertex v) {
        return regionForVertex[v.getIndex()];
    }

}
