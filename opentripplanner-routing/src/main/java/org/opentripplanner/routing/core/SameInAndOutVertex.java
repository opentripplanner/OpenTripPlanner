package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.List;


public class SameInAndOutVertex extends AbstractVertex {

    private static final long serialVersionUID = 1L;

    private List<Edge> _edges = new ArrayList<Edge>();

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
}
