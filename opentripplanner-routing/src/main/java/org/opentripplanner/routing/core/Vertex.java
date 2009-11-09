package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;

public class Vertex implements Serializable {

    private static final long serialVersionUID = 364261663335739528L;

    public Vector<Edge> outgoing;

    public Vector<Edge> incoming;

    public String label;

    public Class type;

    double x, y;

    static final double COS_MAX_LAT = Math.cos(46 * Math.PI / 180);

    static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

    public Vertex(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.outgoing = new Vector<Edge>();
        this.incoming = new Vector<Edge>();
    }

    public double distance(Vertex v) {

        double xd = v.x - x;
        double yd = v.y - y;
        return Math.sqrt(xd * xd + yd * yd) * METERS_PER_DEGREE_AT_EQUATOR * COS_MAX_LAT;

        /* This is more accurate but slower */
        // return GtfsLibrary.distance(y, x, v.y, v.x);
    }

    public Coordinate getCoordinate() {
        return new Coordinate(x, y);
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
}