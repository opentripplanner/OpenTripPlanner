package org.opentripplanner.routing.core;

import java.util.Vector;

import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class AbstractVertex implements Vertex {

    private static final long serialVersionUID = 1L;

    public final String label;

    public final Class<?> type;

    private final double y;

    private final double x;

    static final double COS_MAX_LAT = Math.cos(46 * Math.PI / 180);

    static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

    public AbstractVertex(String label, Class<?> type, double x, double y) {
        this.label = label;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double distance(Vertex v) {

        double xd = v.getX() - getX();
        double yd = v.getY() - getY();
        return Math.sqrt(xd * xd + yd * yd) * METERS_PER_DEGREE_AT_EQUATOR * COS_MAX_LAT;

        /* This is more accurate but slower */
        // return GtfsLibrary.distance(y, x, v.y, v.x);
    }

    public double distance(Coordinate c) {
        return DistanceLibrary.distance(getY(), getX(), c.y, c.x);
    }

    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    @Override
    public void setType(Class<?> type) {
        throw new UnsupportedOperationException();
    }

    public void setX(double x) {
        throw new UnsupportedOperationException();
    }

    public void setY(double y) {
        throw new UnsupportedOperationException();
    }

    public void setOutgoing(Vector<Edge> outgoing) {
        throw new UnsupportedOperationException();
    }

    public void setIncoming(Vector<Edge> incoming) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return "<" + this.label + " " + this.type.getName() + ">";
    }
}
