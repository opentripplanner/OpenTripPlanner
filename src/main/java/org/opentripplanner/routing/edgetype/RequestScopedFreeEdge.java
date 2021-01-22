package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.RequestScopedVertex;

public class RequestScopedFreeEdge extends FreeEdge implements RequestScopedEdge {

    public RequestScopedFreeEdge(RequestScopedVertex from, Vertex to) {
        super((Vertex) from, to);

        if (from.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        }
    }

    public RequestScopedFreeEdge(Vertex from, RequestScopedVertex to) {
        super(from, (Vertex) to);

        if (!to.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}
