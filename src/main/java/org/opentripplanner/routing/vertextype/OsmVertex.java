package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.I18NString;

/**
 * A vertex coming from OpenStreetMap.
 * 
 * This class marks something that comes from the street network itself. It is used for
 * linking origins in Analyst to ensure that they are linked to the same locations
 * regardless of changes in the transit network between (or eventually within) graphs.
 */
public class OsmVertex extends IntersectionVertex {
    private static final long serialVersionUID = 1L;

    /** The OSM node ID from whence this came */
    public final long nodeId;

    public OsmVertex(Graph g, String label, double x, double y, long nodeId) {
        super(g, label, x, y);
        this.nodeId = nodeId;
    }

    public OsmVertex(Graph g, String label, double x, double y, long nodeId, I18NString name) {
        super(g, label, x, y, name);
        this.nodeId = nodeId;
    }
}
