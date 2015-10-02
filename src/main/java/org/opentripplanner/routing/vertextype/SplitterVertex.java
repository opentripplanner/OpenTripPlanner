package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex representing a place along a street between two intersections that is not derived from an OSM node,
 * but is instead the result of breaking that street segment into two pieces in order to connect it to
 * a transit stop.
 */
public class SplitterVertex extends IntersectionVertex {
    /** The OSM node ID of the intersection before this split vertex */
    public final long previousNodeId;

    /** The OSM node ID of the intersection after this split vertex */
    public final long nextNodeId;

    public SplitterVertex(Graph g, String label, double x, double y, StreetEdge streetEdge) {
        super(g, label, x, y);
        // splitter vertices don't represent something that exists in the world, so traversing them is
        // always free.
        this.freeFlowing = true;

        // we have to save the IDs of the OSM nodes so that the split street have the same start and end
        // nodes and can be referenced to OpenTraffic data.
        this.previousNodeId = streetEdge.getStartOsmNodeId();
        this.nextNodeId = streetEdge.getEndOsmNodeId();
    }

    private static final long serialVersionUID = 1L;

}
