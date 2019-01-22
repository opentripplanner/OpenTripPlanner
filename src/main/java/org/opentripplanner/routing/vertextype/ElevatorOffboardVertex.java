package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;

public class ElevatorOffboardVertex extends StreetVertex {

    private static final long serialVersionUID = 20120209L;

    public ElevatorOffboardVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, new NonLocalizedString(name));
    }

}
