package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SampleVertex;

import java.util.Locale;

/**
 * A temporary (single-request scoped) edge that connects a Sample to the street network.
 * Sample is used here in the Analyst sense: a temporary and nondestructive linkage of
 * a single geographic point into the street network.
 */
public class SampleEdge extends Edge implements TemporaryEdge {
    /** length in meters */
    private final int length;

    public SampleEdge(SampleVertex fromv, Vertex v0, int distance) {
        super(fromv, v0);
        this.length = distance;
    }

    public SampleEdge(Vertex v1, SampleVertex tov, int distance) {
        super(v1, tov);
        this.length = distance;
    }

    @Override
    public void dispose() {
        tov.removeIncoming(this);
        fromv.removeOutgoing(this);
    }

    @Override
    /** We want to use exactly the same logic here as is used in propagating to samples */
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWalkDistance(this.length);
        s1.incrementTimeInMilliseconds((int) (1000 * this.length / s0.getOptions().walkSpeed));
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
}
