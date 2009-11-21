package org.opentripplanner.routing.impl;

import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;

public final class DummyReferenceVertex implements Vertex {

    private static final long serialVersionUID = 1L;

    private final String vertexId;

    public DummyReferenceVertex(String vertexId) {
        this.vertexId = vertexId;
    }

    @Override
    public String getLabel() {
        return this.vertexId;
    }

    @Override
    public void addIncoming(Edge ee) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addOutgoing(Edge ee) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double distance(Vertex v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double distance(Coordinate c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Coordinate getCoordinate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDegreeIn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDegreeOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Edge> getIncoming() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Edge> getOutgoing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getX() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getY() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIncoming(Vector<Edge> incoming) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutgoing(Vector<Edge> outgoing) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setType(Class<?> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setX(double x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setY(double y) {
        throw new UnsupportedOperationException();
    }

}
