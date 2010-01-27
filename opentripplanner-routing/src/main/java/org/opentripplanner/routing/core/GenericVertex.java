/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Vector;

import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class GenericVertex implements Vertex {

    private static final long serialVersionUID = 364261663335739528L;

    private Vector<Edge> outgoing;

    private Vector<Edge> incoming;

    public String label;
    
    private String name;

    private String stopId = null;

    public Class<?> type;

    private double y;

    private double x;

    static final double COS_MAX_LAT = Math.cos(46 * Math.PI / 180);

    static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

    public GenericVertex(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.outgoing = new Vector<Edge>();
        this.incoming = new Vector<Edge>();
    }

    public GenericVertex(String label, double x, double y, String name) {
        this(label, x, y);
        this.name = name;
    }
    
    public GenericVertex(String label, double x, double y, String name, String stopId) {
        this(label, x, y);
        this.name = name;
        this.stopId = stopId;
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

    public void setType(Class<?> type) {
        this.type = type;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        incoming.trimToSize();
        outgoing.trimToSize();
        out.defaultWriteObject();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStopId() {
        return stopId;
    }

    public void mergeFrom(Graph graph, GenericVertex other) {
        Iterator<Edge> it = incoming.iterator();
        while(it.hasNext()) {
            Edge edge = it.next();
            if (edge.getFromVertex() == other) {
                it.remove();
            }
        }
        it = outgoing.iterator();
        while(it.hasNext()) {
            Edge edge = it.next();
            if (edge.getToVertex() == other) {
                it.remove();
            }
        }
        for (Edge edge: other.getIncoming()) {
            AbstractEdge aedge = (AbstractEdge) edge;
            if (aedge.getFromVertex() == this) {
                continue;
            }

            aedge.setToVertex(this);
            this.addIncoming(aedge);
        }
        for (Edge edge : other.getOutgoing()) {
            AbstractEdge aedge = (AbstractEdge) edge;
            if (aedge.getToVertex() == this) {
                continue;
            }
            aedge.setFromVertex(this);
            this.addOutgoing(aedge);
        }
        graph.vertices.remove(other.label);
    }
}
