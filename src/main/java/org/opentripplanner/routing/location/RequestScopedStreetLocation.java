package org.opentripplanner.routing.location;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.RequestScopedEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.RequestScopedVertex;
import org.opentripplanner.util.I18NString;

/**
 * This represents the origin or destination of a single request. These are linked to the street
 * graph and discarded after the request has finished.
 */
final public class RequestScopedStreetLocation extends StreetLocation implements
    RequestScopedVertex {
    final private boolean endVertex;

    public RequestScopedStreetLocation(String id, Coordinate nearestPoint, I18NString name,
                                   boolean endVertex) {
        super(id, nearestPoint, name);
        this.endVertex = endVertex;
    }

    @Override
    public void addIncoming(Edge edge) {
        if (edge instanceof RequestScopedEdge) {
            if (endVertex) {
                super.addIncoming(edge);
            } else {
                throw new UnsupportedOperationException("Can't add incoming edge to start vertex");
            }
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
        }
    }

    @Override
    public void addOutgoing(Edge edge) {
        if (edge instanceof RequestScopedEdge) {
            if (endVertex) {
                throw new UnsupportedOperationException("Can't add outgoing edge to end vertex");
            } else {
                super.addOutgoing(edge);
            }
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
        }
    }

    @Override
    public boolean isEndVertex() {
        return endVertex;
    }
}
