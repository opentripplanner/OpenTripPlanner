package org.opentripplanner.model;

import org.opentripplanner.routing.graph.Edge;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a transfer between stops that does not take the street network into account.
 *
 * TODO these should really have a set of valid modes in case bike vs. walk transfers are different
 * TODO Should we just store the NearbyStop as a field here, or even switch to using it instead
 * where this class is used
 */
public class SimpleTransfer implements Serializable {
    private static final long serialVersionUID = 20200316L;
    public final StopLocation from;
    public final StopLocation to;

    private double effectiveWalkDistance;
    
    private List<Edge> edges;

    public SimpleTransfer(StopLocation from, StopLocation to, double effectiveWalkDistance, List<Edge> edges) {
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
