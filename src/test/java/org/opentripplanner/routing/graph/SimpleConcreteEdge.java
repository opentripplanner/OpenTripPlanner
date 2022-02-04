package org.opentripplanner.routing.graph;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.util.I18NString;

public class SimpleConcreteEdge extends Edge {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor without ID.
     * 
     * @param v1
     * @param v2
     */
    public SimpleConcreteEdge(Vertex v1, Vertex v2) {
        super(v1, v2);
    }

    @Override
    public State traverse(State s0) {
        double d = getDistanceMeters();
        TraverseMode mode = s0.getNonTransitMode();
        int t = (int) (d / s0.getOptions().getSpeed(mode, false));
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(t);
        s1.incrementWeight(d);
        return s1.makeState();
    }

    @Override
    public I18NString getName() {
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
