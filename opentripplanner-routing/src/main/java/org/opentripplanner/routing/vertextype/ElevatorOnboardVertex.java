package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

public class ElevatorOnboardVertex extends StreetVertex {

    private static final long serialVersionUID = 20120209L;

    public ElevatorOnboardVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, name);
    }

}
