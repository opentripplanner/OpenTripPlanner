package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * The cost of traversing an intersection is constant.
 * @author avi
 */
public class ConstantIntersectionTraversalCostModel extends AbstractIntersectionTraversalCostModel {
    
    private double cost;
    
    /**
     * All traversal costs are equal to the passed-in constant.
     */
    public ConstantIntersectionTraversalCostModel(double cost) {
        this.cost = cost;
    }
    
    /**
     * Convenience constructor for no cost.
     */
    public ConstantIntersectionTraversalCostModel() {
        this(0.0);
    }
    
    @Override
    public double computeTraversalCost(IntersectionVertex v, StreetEdge from, StreetEdge to, TraverseMode mode,
            RoutingRequest options, float fromSpeed, float toSpeed) {
        return cost;
    }

}
