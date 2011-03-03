package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
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
    public TraverseResult traverse(State s0, TraverseOptions options) {
        State s1 = s0.clone();
        s1.incrementTimeInSeconds(seconds);
        return new TraverseResult(weight, s1,this);
    }
    
    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions options) {
        State s1 = s0.clone();
        s1.incrementTimeInSeconds(-seconds);
        return new TraverseResult(weight, s1,this);
    }
}