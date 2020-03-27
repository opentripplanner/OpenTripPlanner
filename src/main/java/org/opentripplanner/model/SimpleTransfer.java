package org.opentripplanner.model;

import org.opentripplanner.routing.graph.Edge;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a transfer between stops that does not take the street network into account.
 *
 * TODO these should really have a set of valid modes in case bike vs. walk transfers are different
 */
public class SimpleTransfer implements Serializable {
    private static final long serialVersionUID = 20200316L;
    public final Stop from;
    public final Stop to;

    private double effectiveWalkDistance;
    
    private List<Edge> edges;

    public SimpleTransfer(Stop from, Stop to, double effectiveWalkDistance, List<Edge> edges) {
        this.from = from;
        this.to = to;
        this.effectiveWalkDistance = effectiveWalkDistance;
        this.edges = edges;
    }

    public String getName() {
        return from + " => " + to;
    }

    public double getDistanceMeters() {
        return edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    }

    public double getEffectiveWalkDistance(){
    	return this.effectiveWalkDistance;
    }

    public List<Edge> getEdges() { return this.edges; }

    @Override
    public String toString() {
        return "SimpleTransfer " + getName();
    }
}
