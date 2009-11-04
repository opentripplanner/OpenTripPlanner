package org.opentripplanner.jags.core;

import java.io.Serializable;
import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;

public class Vertex extends AbstractVertex implements Serializable {

    private static final long serialVersionUID = 364261663335739528L;

    public Vector<Edge> outgoing;

    public Vector<Edge> incoming;

    public String label;

    public Class type;

    double x, y;

    public Vertex(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.outgoing = new Vector<Edge>();
        this.incoming = new Vector<Edge>();
    }

    public double distance(double x, double y) {
        return Math.pow((Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2)), 0.5);
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