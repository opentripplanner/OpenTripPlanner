package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

public class TemporaryFreeEdge extends FreeEdge implements TemporaryEdge {
    final private boolean endEdge;

    public TemporaryFreeEdge(TemporaryVertex from, Vertex to) {
        super((Vertex) from, to);

        if (from.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else {
            endEdge = false;
        }
    }

    public TemporaryFreeEdge(Vertex from, TemporaryVertex to) {
        super(from, (Vertex) to);

        if (to.isEndVertex()) {
            endEdge = true;
        } else {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public void dispose() {
        if (endEdge) {
            fromv.removeOutgoing(this);
        } else {
            tov.removeIncoming(this);
        }
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}
