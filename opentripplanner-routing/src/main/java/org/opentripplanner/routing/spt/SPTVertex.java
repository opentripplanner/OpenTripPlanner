package org.opentripplanner.routing.spt;

import java.util.Vector;

import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;

public class SPTVertex extends GenericVertex {
    
    private static final long serialVersionUID = -4422788581123655293L;

    public SPTEdge incoming;

    public Vector<SPTEdge> outgoing;

    public Vertex mirror;

    public State state;
    
    public TraverseOptions options;

    public double weightSum;

    SPTVertex(Vertex mirror, State state, double weightSum, TraverseOptions options) {
        super(mirror.getLabel(), mirror.getX(), mirror.getY());
        this.mirror = mirror;
        this.state = state;
        this.weightSum = weightSum;
        this.options = options;
        this.outgoing = new Vector<SPTEdge>();
    }

    public void addOutgoing(SPTEdge ee) {
        this.outgoing.add(ee);
    }

    public void setParent(SPTVertex parent, Edge ep) {
        // remove this edge from outgoing list of previous parent
        if (incoming != null) {
            incoming.fromv.outgoing.remove(incoming);
        }
        incoming = new SPTEdge(parent, this, ep);
        parent.outgoing.add(incoming);
    }

    public String toString() {
        return this.mirror.getLabel() + " (" + this.weightSum + ")";
    }

}