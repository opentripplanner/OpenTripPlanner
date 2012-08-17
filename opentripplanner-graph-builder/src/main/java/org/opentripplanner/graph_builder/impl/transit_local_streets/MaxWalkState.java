package org.opentripplanner.graph_builder.impl.transit_local_streets;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class MaxWalkState extends State {

    public MaxWalkState(Vertex v, RoutingRequest req) {
        super(v, req);
    }
    
    @Override
    public StateEditor edit(Edge e) {
        return new MaxWalkStateEditor(this, e);
    }
    
    class MaxWalkStateEditor extends StateEditor {

        public MaxWalkStateEditor(State parent, Edge e) {
            super(parent, e);
        }
        
        public boolean weHaveWalkedTooFar(RoutingRequest options) {
            //non-transit modes too
            return child.getWalkDistance() >= options.maxWalkDistance;
        }
    }
}
