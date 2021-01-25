package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.RequestScopedVertex;

/**
 * This is used for the linking the origin and destination of a request to the street graph.
 */
public class RequestScopedFreeEdge extends FreeEdge implements RequestScopedEdge {

    public RequestScopedFreeEdge(RequestScopedVertex from, Vertex to) {
        super((Vertex) from, to);

        if (from.isEndVertex()) {
            throw new IllegalStateException("A request scoped edge is directed away from an end "
                + "vertex");
        }
    }

    public RequestScopedFreeEdge(Vertex from, RequestScopedVertex to) {
        super(from, (Vertex) to);

        if (!to.isEndVertex()) {
            throw new IllegalStateException("A reequst scoped edge is directed towards a start "
                + "vertex");
        }
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}
