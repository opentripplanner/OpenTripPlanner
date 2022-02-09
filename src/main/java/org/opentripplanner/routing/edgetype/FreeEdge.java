package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.util.I18NString;

/**
 * An edge that costs nothing to traverse. Used for connecting intersection vertices to the main
 * edge-based graph.
 * 
 * @author novalis
 * 
 */
public class FreeEdge extends Edge {

    private static final long serialVersionUID = 3925814840369402222L;

    public FreeEdge(Vertex from, Vertex to) {
        super(from, to);
    }

    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(1);
        s1.setBackMode(null);
        return s1.makeState();
    }

    @Override
    public double getDistanceMeters() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public I18NString getName() {
        return null;
    }

    public String toString() {
        return "FreeEdge(" + fromv + " -> " + tov + ")";
    }
}
