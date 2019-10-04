package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

/**
 * TODO: decide what to do with this. Currently temporary vertices have only incoming or outgoing edges
 * But this one needs to have both since different vertex is start vertex
 * Created by mabu on 20.5.2016.
 */
public class TemporarySplitterVertex extends SplitterVertex implements TemporaryVertex {

    private boolean wheelchairAccessible;

    final private boolean endVertex;

    public TemporarySplitterVertex(String label, double x, double y, StreetEdge streetEdge, boolean endVertex) {
        super(null, label, x, y, streetEdge);
        this.endVertex = endVertex;
        this.wheelchairAccessible = streetEdge.isWheelchairAccessible();
    }

    @Override
    public void addIncoming(Edge edge) {

        if (edge instanceof TemporaryEdge) {
            super.addIncoming(edge);
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
        }
    }

    @Override
    public void addOutgoing(Edge edge) {
        if (edge instanceof TemporaryEdge) {
            super.addOutgoing(edge);
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
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
