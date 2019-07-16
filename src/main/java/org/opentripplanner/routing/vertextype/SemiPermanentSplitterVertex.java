package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.SemiPermanentPartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

/**
 * This class models a vertex that is used to connect split StreetEdges to semi-permanent vertices such as bike rental
 * stations.
 */
public class SemiPermanentSplitterVertex extends SplitterVertex {

    private boolean wheelchairAccessible;

    public SemiPermanentSplitterVertex(Graph g, String label, double x, double y, StreetEdge streetEdge) {
        super(g, label, x, y, streetEdge);
        this.wheelchairAccessible = streetEdge.isWheelchairAccessible();
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }
}
