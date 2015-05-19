package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex coming from OpenStreetMap.
 * 
 * This class marks something that comes from the street network itself. It is used for
 * linking origins in Analyst to ensure that they are linked to the same locations
 * regardless of changes in the transit network between (or eventually within) graphs.
 */
public class OsmVertex extends IntersectionVertex {
    private static final long serialVersionUID = 1L;

    public OsmVertex(Graph g, String label, double x, double y) {
        super(g, label, x, y);
    }

    public OsmVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, name);
    }
}
