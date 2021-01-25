package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.RequestScopedVertex;
import org.opentripplanner.util.I18NString;

final public class RequestScopedPartialStreetEdge extends PartialStreetEdge implements
    RequestScopedEdge {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new partial street edge along the given 'parentEdge' from 'v1' to 'v2'.
     * If the length is negative, a new length is calculated from the geometry.
     * The elevation data is calculated using the 'parentEdge' and given 'length'.
     */
    public RequestScopedPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        // Assert that the edge is going in the right direction [only possible if vertex is temporary]
        assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(v1);
        assertEdgeIsDirectedTowardsTemporaryEndVertex(v2);
    }

    /**
     * Create a new partial street edge along the given 'parentEdge' from 'v1' to 'v2'.
     * The length is calculated using the provided geometry.
     * The elevation data is calculated using the 'parentEdge' and the calculated 'length'.
     */
    RequestScopedPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, I18NString name) {
        super(parentEdge, v1, v2, geometry, name);

        // Assert that the edge is going in the right direction [only possible if vertex is temporary]
        assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(v1);
        assertEdgeIsDirectedTowardsTemporaryEndVertex(v2);
    }

    @Override
    public String toString() {
        return "RequestScopedPartialStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
                + this.getToVertex() + " length=" + this.getDistanceMeters() + " carSpeed="
                + this.getCarSpeed() + " parentEdge=" + super.getParentEdge() + ")";
    }

    private void assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(StreetVertex v1) {
        if(v1 instanceof RequestScopedVertex) {
            if (((RequestScopedVertex)v1).isEndVertex()) {
                throw new IllegalStateException("A temporary edge is directed away from an end vertex");
            }
        }
    }

    private void assertEdgeIsDirectedTowardsTemporaryEndVertex(StreetVertex v2) {
        if(v2 instanceof RequestScopedVertex) {
            if (!((RequestScopedVertex)v2).isEndVertex()) {
                throw new IllegalStateException("A temporary edge is directed towards a start vertex");
            }
        }
    }
}
