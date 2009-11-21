package org.opentripplanner.routing.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.opentripplanner.routing.impl.DummyReferenceVertex;

public abstract class AbstractEdge implements Edge, Serializable {

    private static final long serialVersionUID = 1L;

    private Vertex fromv, tov;
    
    public void replaceDummyVertices(Graph graph) {
        if( fromv instanceof DummyReferenceVertex)
            fromv = graph.getVertex(((DummyReferenceVertex) fromv).getLabel());
        if( tov instanceof DummyReferenceVertex)
            tov = graph.getVertex(((DummyReferenceVertex) tov).getLabel());
    }

    public String toString() {
        return fromv.getLabel() + "-> " + tov.getLabel();
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        if( fromv != null)
            fromv = new DummyReferenceVertex(fromv.getLabel());
        if( tov != null)
            tov = new DummyReferenceVertex(tov.getLabel());
        out.defaultWriteObject();
    }

}
