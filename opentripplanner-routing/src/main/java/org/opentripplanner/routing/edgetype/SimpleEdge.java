package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.Vertex;

public class SimpleEdge extends FreeEdge {
    private static final long serialVersionUID = 1L;
    private double weight;
    private int seconds;

    public SimpleEdge (Vertex v1, Vertex v2, double weight, int seconds) {
        super(v1, v2);
        this.weight = weight;
        this.seconds = seconds;
    }
    
    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(seconds);
        s1.incrementWeight(weight);
        return s1.makeState();
    }
    
    @Override
    public State traverseBack(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(-seconds);
        s1.incrementWeight(weight);
        return s1.makeState();
    }
}