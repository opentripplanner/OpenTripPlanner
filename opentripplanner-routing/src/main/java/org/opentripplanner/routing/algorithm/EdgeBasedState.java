package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class EdgeBasedState extends org.opentripplanner.routing.impl.raptor.MaxWalkState implements Cloneable {

    public Edge outgoing;
    
    public EdgeBasedState(Vertex v, RoutingRequest req) {
        super(v, req);
    }

    @Override
    public EdgeBasedStateEditor edit(Edge e) {
        return new EdgeBasedStateEditor(this, e);
    }

    @Override
    public StateEditor edit(Edge e, EdgeNarrative en) {
        return new EdgeBasedStateEditor(this, e, en);
    }

    @Override
    public boolean dominates(State other) {
        return (backEdge == other.getBackEdge() || !(backEdge instanceof PlainStreetEdge) 
                  || (((PlainStreetEdge) backEdge).getTurnRestrictions().isEmpty()))
                && walkDistance <= other.getWalkDistance() * 1.05
                && this.getTime() <= other.getTime();
    }
    
    @Override
    public EdgeBasedState clone() {
        return (EdgeBasedState) super.clone();
    }

    public static class EdgeBasedStateEditor extends StateEditor {

        public EdgeBasedStateEditor(RoutingRequest options, Vertex v) {
            super();
            child = new EdgeBasedState(v, options);
            child.stateData = new StateData();
        }

        @Override
        public boolean parsePath(State state) {
            return true;
        }
        
        public EdgeBasedStateEditor(EdgeBasedState parent, Edge e) {
            super(parent, e);
            ((EdgeBasedState) child).outgoing = parent.outgoing;
        }

        public EdgeBasedStateEditor(EdgeBasedState parent, Edge e, EdgeNarrative en) {
            super(parent, e, en);
            ((EdgeBasedState) child).outgoing = parent.outgoing;
        }
        
        public boolean weHaveWalkedTooFar(RoutingRequest options) {
            return child.getWalkDistance() >= options.maxWalkDistance;
        }
    }

    /**
     * This should only be used in the edge-based dijsktra
     * @param weight
     * @param time
     */
    public void turn(double weight, long time) {
      weight += weight;
      time += time;
    }
}
