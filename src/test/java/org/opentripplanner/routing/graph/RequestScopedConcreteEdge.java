package org.opentripplanner.routing.graph;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;

import org.locationtech.jts.geom.LineString;
import java.util.Locale;
import org.opentripplanner.routing.edgetype.RequestScopedEdge;
import org.opentripplanner.routing.vertextype.RequestScopedVertex;

public class RequestScopedConcreteEdge extends Edge implements RequestScopedEdge {

    public RequestScopedConcreteEdge(RequestScopedVertex v1, Vertex v2) {
        super((Vertex) v1, v2);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        }
    }

    public RequestScopedConcreteEdge(Vertex v1, RequestScopedVertex v2) {
        super(v1, (Vertex) v2);

        if (!v2.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public State traverse(State s0) {
        double d = getDistanceMeters();
        TraverseMode mode = s0.getNonTransitMode();
        int t = (int) (d / s0.getOptions().getSpeed(mode));
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(t);
        s1.incrementWeight(d);
        return s1.makeState();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getName(Locale locale) {
        return null;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public double getDistanceMeters() {
        return SphericalDistanceLibrary.distance(getFromVertex().getCoordinate(), getToVertex().getCoordinate());
    }
}
