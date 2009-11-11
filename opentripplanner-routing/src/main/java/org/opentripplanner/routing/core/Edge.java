package org.opentripplanner.routing.core;

import java.io.Serializable;

import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.edgetype.Traversable;

public class Edge extends AbstractEdge implements Serializable {
    private static final long serialVersionUID = 2847531383395983317L;

    public Vertex fromv;

    public Vertex tov;

    public Traversable payload;

    public Edge(Vertex fromv, Vertex tov, Traversable payload) {
        this.fromv = fromv;
        this.tov = tov;
        this.payload = payload;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException {
        TraverseResult tr = this.payload.traverse(s0, wo);
        return tr;
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException {
        TraverseResult tr = this.payload.traverseBack(s0, wo);
        return tr;
    }

    public String toString() {
        return fromv.label + " -" + payload.toString() + "-> " + tov.label;
    }
}