package org.opentripplanner.routing.spt;

import java.util.Vector;

import org.opentripplanner.routing.core.AbstractVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Traversable;

public class SPTVertex extends AbstractVertex {
    public SPTEdge incoming;

    public Vector<SPTEdge> outgoing;

    public Vertex mirror;

    public State state;

    public double weightSum;

    SPTVertex(Vertex mirror, State state, double weightSum) {
        this.mirror = mirror;
        this.state = state;
        this.weightSum = weightSum;
        this.outgoing = new Vector<SPTEdge>();
    }

    public void addOutgoing(SPTEdge ee) {
        this.outgoing.add(ee);
    }

    public void setParent(SPTVertex parent, Traversable ep) {
        // remove this edge from outgoing list of previous parent
        if (incoming != null) {
            incoming.fromv.outgoing.remove(incoming);
        }
        incoming = new SPTEdge(parent, this, ep);
        parent.outgoing.add(incoming);
    }

    public String toString() {
        return this.mirror.label + " (" + this.weightSum + ")";
    }

}