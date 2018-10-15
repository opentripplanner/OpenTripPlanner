package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex acting as a starting point for planning a trip while onboard an existing trip.
 * 
 * @author laurent
 */
public class OnboardDepartVertex extends Vertex implements TemporaryVertex {
    private static final long serialVersionUID = -6721280275560962711L;

    public OnboardDepartVertex(String label, double lon, double lat) {
        super(null, label, lon, lat, new NonLocalizedString(label));
    }

    @Override
    public void addIncoming(Edge edge) {
        throw new UnsupportedOperationException("Can't add incoming edge to start vertex");
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
        return false;
    }
}
