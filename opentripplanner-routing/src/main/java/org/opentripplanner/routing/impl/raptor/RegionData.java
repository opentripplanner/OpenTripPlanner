package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class RegionData implements Serializable {
    private static final long serialVersionUID = 4835563169492792969L;

    public int[] regionForVertex;

    public double[][] minWalk;

    public double[][] minTime;

    public List<Integer> getRegionsForVertex(Vertex vertex) {
        int index = vertex.getIndex();
        if (index < regionForVertex.length && regionForVertex[index] > 0) {
            return Arrays.asList(regionForVertex[index]);
        }
        ArrayList<Integer> regions = new ArrayList<Integer>();
        for (Edge e : vertex.getOutgoing()) {
            Vertex tov = e.getToVertex();
            for (int region : getRegionsForVertex(tov)) {
                if (regions.contains(region))
                    continue;
                regions.add(region);
            }
        }
        return regions;
    }

}
