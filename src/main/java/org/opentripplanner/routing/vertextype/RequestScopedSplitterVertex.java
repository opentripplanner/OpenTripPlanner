package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.RequestScopedEdge;
import org.opentripplanner.routing.graph.Edge;

/**
 * This is the vertex that is created when a request splits a street edge non-destructively in order
 * to link origin and destination points to the street graph.
 */
public class RequestScopedSplitterVertex extends SplitterVertex implements RequestScopedVertex {

    private boolean wheelchairAccessible;

    final private boolean endVertex;

    public RequestScopedSplitterVertex(String label, double x, double y, StreetEdge streetEdge, boolean endVertex) {
        super(null, label, x, y, streetEdge);
        this.endVertex = endVertex;
        this.wheelchairAccessible = streetEdge.isWheelchairAccessible();
    }

    @Override
    public void addIncoming(Edge edge) {

        if (edge instanceof RequestScopedEdge) {
            super.addIncoming(edge);
        } else {
            throw new UnsupportedOperationException("Can't add edge that is not request scoped to "
                + "request scoped vertex vertex");
        }
    }

    @Override
    public void addOutgoing(Edge edge) {
        if (edge instanceof RequestScopedEdge) {
            super.addOutgoing(edge);
        } else {
            throw new UnsupportedOperationException("Can't add edge that is not request scoped to "
                + "request scoped vertex");
        }
    }

    @Override
    public boolean isEndVertex() {
        return endVertex;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }
}
