package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;

public interface Vertex extends Serializable {

    public String getLabel();

    public double distance(Vertex v);

    public double distance(Coordinate c);

    public Coordinate getCoordinate();

    public int getDegreeOut();

    public int getDegreeIn();

    public void addIncoming(Edge ee);

    public void addOutgoing(Edge ee);

    public String toString();

    public void setX(double x);

    public double getX();

    public void setY(double y);

    public double getY();

    public void setOutgoing(Vector<Edge> outgoing);

    public Iterable<Edge> getOutgoing();

    public void setIncoming(Vector<Edge> incoming);

    public Iterable<Edge> getIncoming();

    public Class<?> getType();

    public void setType(Class<?> type);

    public String getName();

}