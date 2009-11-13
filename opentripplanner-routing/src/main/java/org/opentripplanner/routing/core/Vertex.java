package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Vector;

import org.opentripplanner.gtfs.GtfsLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class Vertex implements Serializable {

    private static final long serialVersionUID = 364261663335739528L;

    public Vector<Edge> outgoing;

    public Vector<Edge> incoming;

    public String label;

    public Class<?> type;

    private double y;

    private double x;

    static final double COS_MAX_LAT = Math.cos(46 * Math.PI / 180);

    static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

    public Vertex(String label, double x, double y) {
        this.label = label;
        this.setX(x);
        this.setY(y);
        this.outgoing = new Vector<Edge>();
        this.incoming = new Vector<Edge>();
    }

    public double distance(Vertex v) {

        double xd = v.getX() - getX();
        double yd = v.getY() - getY();
        return Math.sqrt(xd * xd + yd * yd) * METERS_PER_DEGREE_AT_EQUATOR * COS_MAX_LAT;

        /* This is more accurate but slower */
        // return GtfsLibrary.distance(y, x, v.y, v.x);
    }
    

    public double distance(Coordinate c) {
        return GtfsLibrary.distance(getY(), getX(), c.y, c.x);
    }

    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    public int getDegreeOut() {
        return this.outgoing.size();
    }

    public int getDegreeIn() {
        return this.incoming.size();
    }

    public void addIncoming(Edge ee) {
        this.incoming.add(ee);
    }

    public void addOutgoing(Edge ee) {
        this.outgoing.add(ee);
    }

    public String toString() {
        return "<" + this.label + " " + this.outgoing.size() + " " + this.incoming.size() + ">";
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getX() {
        return x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getY() {
        return y;
    }

}