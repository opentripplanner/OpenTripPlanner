package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

public class ExitVertex extends OsmVertex {
    
    private static final long serialVersionUID = -1403959315797898914L;
    private String exitName;
    
    public ExitVertex(Graph g, String label, double x, double y, long nodeId) {
        super(g, label, x, y, nodeId);
    }

    public String getExitName() {
        return exitName;
    }

    public void setExitName(String exitName) {
        this.exitName = exitName;
    }

    public String toString() {
        return "ExitVertex(" + super.toString() + ")";
    }
}
