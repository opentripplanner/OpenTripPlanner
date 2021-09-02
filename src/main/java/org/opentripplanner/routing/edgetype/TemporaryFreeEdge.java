package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

public class TemporaryFreeEdge extends FreeEdge implements TemporaryEdge {

    public TemporaryFreeEdge(TemporaryVertex from, Vertex to) {
        super((Vertex) from, to);

        if (from.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        }
    }

    public TemporaryFreeEdge(Vertex from, TemporaryVertex to) {
        super(from, (Vertex) to);

        if (!to.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(1);
        s1.setBackMode(null);

        if (s0.isBikeRentingFromStation()
                && s0.mayKeepRentedBicycleAtDestination()
                && s0.getOptions().allowKeepingRentedBicycleAtDestination) {
            s1.incrementWeight(s0.getOptions().keepingRentedBicycleAtDestinationCost);
        }

        return s1.makeState();
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}
