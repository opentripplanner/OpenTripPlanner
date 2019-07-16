package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.util.I18NString;

/**
 * This class models a StreetEdge that was non-destructively split from another StreetEdge for the purposes of modeling
 * StreetEdges that should only be valid for a single request. These edges typically include edges used to link the
 * origin or destination of a routing request to the graph.
 */
public class TemporaryPartialStreetEdge extends PartialStreetEdge implements TemporaryEdge {
    public TemporaryPartialStreetEdge(
        StreetEdge parentEdge,
        StreetVertex v1,
        StreetVertex v2,
        LineString geometry,
        I18NString name,
        double length
    ) {
        super(parentEdge, v1, v2, geometry, name, length);

        // Assert that the edge is going in the right direction [only possible if vertex is temporary]
        assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(v1);
        assertEdgeIsDirectedTowardsTemporaryEndVertex(v2);
    }

    private void assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(StreetVertex v1) {
        if(v1 instanceof TemporaryVertex) {
            if (((TemporaryVertex)v1).isEndVertex()) {
                throw new IllegalStateException("A temporary edge is directed away from an end vertex");
            }
        }
    }

    private void assertEdgeIsDirectedTowardsTemporaryEndVertex(StreetVertex v2) {
        if(v2 instanceof TemporaryVertex) {
            if (!((TemporaryVertex)v2).isEndVertex()) {
                throw new IllegalStateException("A temporary edge is directed towards a start vertex");
            }
        }
    }

    @Override
    public String toString() {
        return "TemporaryPartialStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
            + this.getToVertex() + " length=" + this.getDistance() + " carSpeed="
            + this.getCarSpeed() + " parentEdge=" + parentEdge + ")";
    }
}
