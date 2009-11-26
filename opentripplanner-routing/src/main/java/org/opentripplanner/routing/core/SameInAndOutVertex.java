package org.opentripplanner.routing.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class SameInAndOutVertex extends AbstractVertex {

    private static final long serialVersionUID = 1L;

    private ArrayList<Edge> _edges = new ArrayList<Edge>();

    public SameInAndOutVertex(String label, Class<?> type, double x, double y) {
        super(label, type, x, y);
    }

    public void addEdge(Edge edge) {
        _edges.add(edge);
    }

    @Override
    public void addIncoming(Edge ee) {
        addEdge(ee);
    }

    @Override
    public void addOutgoing(Edge ee) {
        addEdge(ee);
    }

    @Override
    public int getDegreeIn() {
        return _edges.size();
    }

    @Override
    public int getDegreeOut() {
        return _edges.size();
    }

    @Override
    public Iterable<Edge> getIncoming() {
        return _edges;
    }

    @Override
    public Iterable<Edge> getOutgoing() {
        return _edges;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        _edges.trimToSize();
        out.defaultWriteObject();
    }
}
