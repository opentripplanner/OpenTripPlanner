package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

public class TransitStopStreetVertex extends IntersectionVertex {

    public String stopCode;

    public TransitStopStreetVertex(Graph g, String label, double x, double y, String name, String stopCode) {
        super(g, label, x, y, name);
        this.stopCode = stopCode;
    }
}
