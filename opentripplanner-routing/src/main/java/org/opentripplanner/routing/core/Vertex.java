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
import java.io.Serializable;
import java.util.Collection;

import org.onebusaway.gtfs.model.AgencyAndId;

import com.vividsolutions.jts.geom.Coordinate;
import static org.opentripplanner.common.IterableLibrary.cast;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.impl.DistanceLibrary;

public class Vertex implements Cloneable, Serializable{

    private static final long serialVersionUID = 20111019;
    
    private static int maxIndex = 0;

    public final String label;
    
    protected String name;

    /**
     * For vertices that represent stops, the passenger-facing stop ID (for systems like TriMet that
     * have this feature).
     */
    private AgencyAndId stopId = null;

    /**
     * The latitude of the vertex
     */
    private double y;

    /**
     * The longitude of the vertex
     */
    private double x;

    /**
     * Each vertex has a unique index that serves as a hashcode or table index
     */
    int index;

    /**
     * Distance to the closest transit stop in meters
     */
    private double distanceToNearestTransitStop = 0;

    private transient ArrayList<Edge> incoming = new ArrayList<Edge>();

    private transient ArrayList<Edge> outgoing = new ArrayList<Edge>();

    public void addOutgoing(Edge ee) {
        outgoing.add(ee);
    }
    
    public void addIncoming(Edge ee) {
        incoming.add(ee);
    }
    
    public void removeOutgoing(Edge ee) {
        outgoing.remove(ee);
    }
    
    public void removeIncoming(Edge ee) {
        incoming.remove(ee);
    }
    
    public int getDegreeIn() {
        return incoming.size();
    }

    public int getDegreeOut() {
        return outgoing.size();
    }

    public Collection<Edge> getIncoming() {
        return incoming;
    }

    public Collection<Edge> getOutgoing() {
        return outgoing;
    }

    public void removeAllEdges() {
        for (Edge e : outgoing) {
            if (e instanceof DirectEdge) {
                DirectEdge edge = (DirectEdge) e;
                // this used to grab the graphvertex by label... now it could possibly be a vertex
                // that is not in the graph
                // Vertex target = vertices.get(edge.getToVertex().getLabel());
                Vertex target = edge.getToVertex();
                if (target != null) {
                    target.removeIncoming(e);
                }
            }
        }
        for (Edge e : incoming) {
            // why only directedges?
            if (e instanceof DirectEdge) {
                DirectEdge edge = (DirectEdge) e;
                // Vertex source = vertices.get(edge.getFromVertex().getLabel());
                Vertex source = edge.getFromVertex();
                if (source != null) {
                    // changed to removeOutgoing (AB)
                    source.removeOutgoing(e);
                }
            }
        }
        incoming = new ArrayList<Edge>();
        outgoing = new ArrayList<Edge>();
    }
    
    public Vertex(String label, Coordinate coord, String name) {
        this(label, coord.x, coord.y, name);
    }

    public Vertex(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.index = maxIndex  ++;
    }

    public Vertex(String label, double x, double y, String name) {
        this(label, x, y);
        this.name = name;
    }
    
    public Vertex(String label, double x, double y, String name, AgencyAndId stopId) {
        this(label, x, y);
        this.name = name;
        this.stopId = stopId;
    }

    /**
     * Distance in meters to the coordinate
     */
    public double distance(Coordinate c) {
        return DistanceLibrary.distance(getY(), getX(), c.y, c.x);
    }

    /**
     * Distance in meters to the vertex
     */
    public double distance(Vertex v) {
        return DistanceLibrary.distance(getY(), getX(), v.getY(), v.getX());
    }
    
    /**
     * Fast, slightly approximated, under-estimated distance in meters to the vertex
     */
    public double fastDistance(Vertex v) {
        return DistanceLibrary.fastDistance(getY(), getX(), v.getY(), v.getX());
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

    /**
     * Every vertex has a label which is globally unique
     */
    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public AgencyAndId getStopId() {
        return stopId;
    }

    /**
     * Merge another vertex into this one.  Useful during graph construction for handling 
     * sequential non-branching streets, and empty dwells.
     */
    public void mergeFrom(Graph graph, Vertex other) {
        // We only support Vertices that are direct edges when merging
        Iterable<DirectEdge> incoming = cast(this.incoming);
        Iterable<DirectEdge> outgoing = cast(this.outgoing);

        //remove incoming edges from other to this
        Iterator<DirectEdge> it = incoming.iterator();
        while(it.hasNext()) {
            DirectEdge edge = it.next();
            if (edge.getFromVertex() == other) {
                it.remove();
            }
        }
        //remove outgoing edges from this to other
        it = outgoing.iterator();
        while(it.hasNext()) {
            DirectEdge edge = it.next();
            if (edge.getToVertex() == other) {
                it.remove();
            }
        }
        //make incoming edges to other point to this
        for (AbstractEdge edge : cast(other.getIncoming(), AbstractEdge.class)) {
            if (edge.getFromVertex() == this) {
                continue;
            }
            edge.setToVertex(this);
            this.addIncoming(edge);
        }
        //add outgoing edges from other to outgoing from this
        for (AbstractEdge edge : cast(other.getOutgoing(), AbstractEdge.class)) {
            if (edge.getToVertex() == this) {
                continue;
            }
            edge.setFromVertex(this);
            this.addOutgoing(edge);
        }
        graph.removeVertex(other);
    }
    
    public int hashCode() {
        return index;
    }

    /* SERIALIZATION */
    
    private void writeObject(ObjectOutputStream out) throws IOException {
// Transient so no need to trim
//        incoming.trimToSize();
//        outgoing.trimToSize();
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.incoming = new ArrayList<Edge>(2);
        this.outgoing = new ArrayList<Edge>(2);
        index = maxIndex++;
    }

    public void setDistanceToNearestTransitStop(double distance) {
        distanceToNearestTransitStop = distance;
    }

    public double getDistanceToNearestTransitStop() {
        return distanceToNearestTransitStop;
    }

    public int getIndex() {
        return index;
    }
    
    public static int getMaxIndex() {
        return maxIndex;
    }

    public List<DirectEdge> getOutgoingStreetEdges() {
        List<DirectEdge> result = new ArrayList<DirectEdge>();
        for (Edge out : this.getOutgoing()) {
            if (!(out instanceof TurnEdge || out instanceof OutEdge || out instanceof PlainStreetEdge)) {
                continue;
            }
            result.add((StreetEdge) out);
        }
        return result;
    }

}