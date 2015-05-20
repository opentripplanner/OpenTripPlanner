package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SampleVertex;

/**
 * A temporary (single-request scoped) edge that connects a Sample to the street network.
 * Sample is used here in the Analyst sense: a temporary and nondestructive linkage of
 * a single geographic point into the street network.
 */
public class SampleEdge extends Edge implements TemporaryEdge {
    /** length in meters */
    private final int length;

    // TODO: implement for end vertex case. Note that you will also need to change dispose(), below.
    public SampleEdge(SampleVertex fromv, Vertex v0, int distance) {
        super(fromv, v0);
        this.length = distance;
    }

    @Override
    public void dispose() {
        // for the time being sample edges are by definition from samples to other vertices
        tov.removeIncoming(this);
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
}
