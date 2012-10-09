package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AreaEdgeList implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 969137349467214074L;
    private List<AreaEdge> edges = new ArrayList<AreaEdge>();

    public List<AreaEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<AreaEdge> edges) {
        this.edges = edges;
    }
    
    public void addEdge(AreaEdge edge) {
        this.edges.add (edge);
    }

    public void removeEdge(AreaEdge edge) {
        this.edges.remove(edge);
    }
    
}
