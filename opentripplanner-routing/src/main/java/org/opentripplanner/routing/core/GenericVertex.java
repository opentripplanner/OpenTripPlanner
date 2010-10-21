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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class GenericVertex implements Vertex, Serializable {

    private static final long serialVersionUID = 364261663335739528L;

    public final String label;
    
    protected String name;

    private String stopId = null;

    private double y;

    private double x;

    private transient int index;

    private static int maxIndex = 0;

    static final double COS_MAX_LAT = Math.cos(49 * Math.PI / 180);

    static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

    public GenericVertex(String label, Coordinate coord, String name) {
        this(label, coord.x, coord.y, name);
    }

    public GenericVertex(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.index = maxIndex  ++;
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

    public double fastDistance(Vertex v) {

        double xd = v.getX() - getX();
        double yd = v.getY() - getY();
        return Math.sqrt(xd * xd + yd * yd) * METERS_PER_DEGREE_AT_EQUATOR * COS_MAX_LAT;

    }

    public double distance(Coordinate c) {
        return DistanceLibrary.distance(getY(), getX(), c.y, c.x);
    }

    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    public String toString() {
        return "<" + this.label + ">";
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

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStopId() {
        return stopId;
    }

    /**
     * Merge another vertex into this one.  Useful during graph construction for handling 
     * sequential non-branching streets, and empty dwells.
     */
    public void mergeFrom(Graph graph, GenericVertex other) {
        GraphVertex gv = graph.getGraphVertex(this);
        Collection<Edge> incoming = gv.incoming;
        Collection<Edge> outgoing = gv.outgoing;
        Iterator<Edge> it = incoming.iterator();
        //remove incoming edges from other to this
        while(it.hasNext()) {
            Edge edge = it.next();
            if (edge.getFromVertex() == other) {
                it.remove();
            }
        }
        //remove outgoing edges from other to this
        it = outgoing.iterator();
        while(it.hasNext()) {
            Edge edge = it.next();
            if (edge.getToVertex() == other) {
                it.remove();
            }
        }
        //make incoming edges to other point to this 
        for (Edge edge: graph.getIncoming(other)) {
            if (edge.getFromVertex() == this) {
                continue;
            }

            edge.setToVertex(this);
            gv.addIncoming(edge);
        }
        //add outgoing edges from other to outgoing from this
        for (Edge edge : graph.getOutgoing(other)) {
            if (edge.getToVertex() == this) {
                continue;
            }
            edge.setFromVertex(this);
            gv.addOutgoing(edge);
        }
        graph.removeVertex(other);
    }
    
    public int hashCode() {
        return index;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        index = maxIndex++;
    }
}
