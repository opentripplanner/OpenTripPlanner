package org.opentripplanner.routing.core;

public abstract class AbstractEdge implements Edge {

    private Vertex fromv, tov;

    public String toString() {
        return fromv.label + "-> " + tov.label;
    }

    public AbstractEdge(Vertex fromv, Vertex tov) {
        this.fromv = fromv;
        this.tov = tov;
    }

    @Override
    public Vertex getFromVertex() {
        return fromv;
    }

    @Override
    public Vertex getToVertex() {
        return tov;
    }
}
