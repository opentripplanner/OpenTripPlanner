package org.opentripplanner.routing.vertextype;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex representing a Sample in the Analyst sense -- a temporary and nondestructive linkage of
 * a single geographic point into the street network.
 */
public class SampleVertex extends StreetVertex implements TemporaryVertex  {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isEndVertex() {
        return false;
    }

    @Override
    public void addIncoming(Edge e) {
        if (!(e instanceof TemporaryEdge)) {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary sample vertex.");
        }

        super.addIncoming(e);
    }

    @Override
    public void addOutgoing(Edge e) {
        if (!(e instanceof TemporaryEdge)) {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary sample vertex.");
        }

        super.addOutgoing(e);
    }

    public SampleVertex (Graph g, Coordinate c) {
        super(g, null, c, null);
    }

    @Override
    public void dispose() {
        for (Object temp : getOutgoing()) {
            ((TemporaryEdge) temp).dispose();
        }
    }

    @Override
    public String getLabel () {
        return "sample-" + getIndex();
    }

}
