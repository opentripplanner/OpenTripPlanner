package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * An interface to a model that computes the costs of turns.
 * 
 * Turn costs are in units of seconds - they represent the expected amount of time it would take to make a turn.
 * 
 * @author avi
 */
public interface IntersectionTraversalCostModel {
    
    /**
     * Compute the cost of turning onto "to" from "from".
     * 
     * @return expected number of seconds the traversal is expected to take.
     */
    public double computeTraversalCost(IntersectionVertex v, StreetEdge from,
            StreetEdge to, TraverseMode mode, RoutingRequest options, float fromSpeed,
            float toSpeed);

}
