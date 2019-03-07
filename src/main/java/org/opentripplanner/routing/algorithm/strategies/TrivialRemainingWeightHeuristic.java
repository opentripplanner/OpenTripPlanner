package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A trivial heuristic that always returns 0, which is always admissible. For use in testing, troubleshooting, and
 * spatial analysis applications where there is no target.
 */
public class TrivialRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 1L;

    @Override
    public void initialize(RoutingRequest options, long abortTime) {}

    @Override
    public double estimateRemainingWeight (State s) {
        return 0;
    }

    @Override
    public void reset() {}
    
    @Override
    public void doSomeWork() {}

}
