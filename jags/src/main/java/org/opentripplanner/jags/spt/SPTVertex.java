package org.opentripplanner.jags.spt;

import java.util.Vector;

import org.opentripplanner.jags.core.AbstractVertex;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Walkable;

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

    public void setParent(SPTVertex parent, Walkable ep) {
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