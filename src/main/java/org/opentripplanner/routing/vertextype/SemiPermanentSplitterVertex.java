package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.SemiPermanentPartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;

/**
 *
 */
public class SemiPermanentSplitterVertex extends SplitterVertex {

    private boolean wheelchairAccessible;

    final private boolean endVertex;

    public SemiPermanentSplitterVertex(String label, double x, double y, StreetEdge streetEdge, boolean endVertex) {
        super(null, label, x, y, streetEdge);
        this.endVertex = endVertex;
        this.wheelchairAccessible = streetEdge.isWheelchairAccessible();
    }

    @Override
    public void addIncoming(Edge edge) {
        if (edge instanceof SemiPermanentPartialStreetEdge) {
            super.addIncoming(edge);
        } else {
            throw new UnsupportedOperationException("Only semi-permanent edges may be added to semi-permanent vertex");
        }
    }

    @Override
    public void addOutgoing(Edge edge) {
        if (edge instanceof SemiPermanentPartialStreetEdge) {
            super.addOutgoing(edge);
        } else {
            throw new UnsupportedOperationException("Only semi-permanent edges may be added to semi-permanent vertex");
        }
    }

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
