package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.I18NString;

/**
 * An interface for modelling all possible vehicle rental vertices.
 */
public abstract class RentalStationVertex extends Vertex {
    protected RentalStationVertex(Graph g, String label, double x, double y) {
        super(g, label, x, y);
    }

    protected RentalStationVertex(Graph g, String label, double x, double y, I18NString name) {
        super(g, label, x, y, name);
    }
}
