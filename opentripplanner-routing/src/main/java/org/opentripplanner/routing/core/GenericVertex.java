package org.opentripplanner.routing.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Vector;

import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class GenericVertex implements Vertex {

    private static final long serialVersionUID = 364261663335739528L;

    private Vector<Edge> outgoing;

    private Vector<Edge> incoming;

    public String label;

    public Class<?> type;

    private double y;

    private double x;

    static final double COS_MAX_LAT = Math.cos(46 * Math.PI / 180);

    static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

    public GenericVertex(String label, double x, double y) {
        this.label = label;
        this.setX(x);
        this.setY(y);
        this.setOutgoing(new Vector<Edge>());
        this.setIncoming(new Vector<Edge>());
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

    public int getDegreeOut() {
        return this.getOutgoing().size();
    }

    public int getDegreeIn() {
        return this.getIncoming().size();
    }

    public void addIncoming(Edge ee) {
        this.getIncoming().add(ee);
    }

    public void addOutgoing(Edge ee) {
        this.getOutgoing().add(ee);
    }

    public String toString() {
        return "<" + this.label + " " + this.getOutgoing().size() + " " + this.getIncoming().size() + ">";
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

    public void setOutgoing(Vector<Edge> outgoing) {
        this.outgoing = outgoing;
    }

    public Vector<Edge> getOutgoing() {
        return outgoing;
    }

    public void setIncoming(Vector<Edge> incoming) {
        this.incoming = incoming;
    }

    public Vector<Edge> getIncoming() {
        return incoming;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public void setType(Class<?> type) {
        this.type = type;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        incoming.trimToSize();
        outgoing.trimToSize();
        out.defaultWriteObject();
    }
}
