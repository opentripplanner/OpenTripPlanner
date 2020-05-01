package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.util.I18NString;

import java.util.List;

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

    /**
     * Partial edges are always partial.
     */
    @Override
    public boolean isPartial() {
        return true;
    }

    /**
     * Have the ID of their parent.
     */
    @Override
    public int getId() {
        return parentEdge.getId();
    }

    /**
     * Have the inbound angle of  their parent.
     */
    @Override
    public int getInAngle() {
        return parentEdge.getInAngle();
    }

    /**
     * Have the outbound angle of  their parent.
     */
    @Override
    public int getOutAngle() {
        return parentEdge.getInAngle();
    }

    /**
     * Have the turn restrictions of  their parent.
     */
    @Override
    protected List<TurnRestriction> getTurnRestrictions(Graph graph) {
        return graph.getTurnRestrictions(parentEdge);
    }

    /**
     * This implementation makes it so that TurnRestrictions on the parent edge are applied to this edge as well.
     */
    @Override
    public boolean isEquivalentTo(Edge e) {
        return (e == this || e == parentEdge);
    }

    @Override
    public boolean isReverseOf(Edge e) {
        Edge other = e;
        if (e instanceof TemporaryPartialStreetEdge) {
            other = ((TemporaryPartialStreetEdge) e).parentEdge;
        }

        // TODO(flamholz): is there a case where a partial edge has a reverse of its own?
        return parentEdge.isReverseOf(other);
    }

    @Override
    public boolean isRoundabout() {
        return parentEdge.isRoundabout();
    }

    /**
     * Returns true if this edge is trivial - beginning and ending at the same point.
     */
    public boolean isTrivial() {
        Coordinate fromCoord = this.getFromVertex().getCoordinate();
        Coordinate toCoord = this.getToVertex().getCoordinate();
        return fromCoord.equals(toCoord);
    }

    public StreetEdge getParentEdge() {
        return parentEdge;
    }

    @Override
    public String toString() {
        return "TemporaryPartialStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
            + this.getToVertex() + " length=" + this.getDistance() + " carSpeed="
            + this.getCarSpeed() + " parentEdge=" + parentEdge + ")";
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

    private void setElevationProfileUsingParents() {
        setElevationProfile(
            ElevationUtils.getPartialElevationProfile(
                getParentEdge().getElevationProfile(), 0, getDistance()
            ),
            false
        );
    }
}
