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

    private AgencyAndId stopId = null;

    private double y;

    private double x;

    private int index;

    private transient int groupIndex = -1;
    
    private double distanceToNearestTransitStop = 0;

    private transient ArrayList<Edge> incoming = new ArrayList<Edge>();

    private transient ArrayList<Edge> outgoing = new ArrayList<Edge>();

    
    /* PUBLIC CONSTRUCTORS */
    
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


    /* PUBLIC METHODS */
    
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

    public String toString() {
        return "<" + this.label + ">";
    }

    public int hashCode() {
        return index;
    }


    /* FIELD ACCESSOR METHODS : READ/WRITE */

    public void addOutgoing(Edge ee) {
        outgoing.add(ee);
    }
    
    public void removeOutgoing(Edge ee) {
        outgoing.remove(ee);
    }

    /** Get a collection containing all the edges leading from this vertex to other vertices. */
    public Collection<Edge> getOutgoing() {
        return outgoing;
    }

    public void addIncoming(Edge ee) {
        incoming.add(ee);
    }
    
    public void removeIncoming(Edge ee) {
        incoming.remove(ee);
    }

    /** Get a collection containing all the edges leading from other vertices to this vertex. */
    public Collection<Edge> getIncoming() {
        return incoming;
    }

    public int getDegreeOut() {
        return outgoing.size();
    }

    public int getDegreeIn() {
        return incoming.size();
    }
    
    public void setDistanceToNearestTransitStop(double distance) {
        distanceToNearestTransitStop = distance;
    }
    
    /** Get the distance from this vertex to the closest transit stop in meters. */
    public double getDistanceToNearestTransitStop() {
        return distanceToNearestTransitStop;
    }

    /** Set the longitude of the vertex */
    public void setX(double x) {
        this.x = x;
    }

    /** Get the longitude of the vertex */
    public double getX() {
        return x;
    }

    /** Set the latitude of the vertex */
    public void setY(double y) {
        this.y = y;
    }

    /** Get the latitude of the vertex */
    public double getY() {
        return y;
    }

    public void setGroupIndex(int groupIndex) {
        this.groupIndex = groupIndex;
    }
    
    public int getGroupIndex() {
        return groupIndex;
    }
    

    /* FIELD ACCESSOR METHODS : READ ONLY */

    /** Every vertex has a label which is globally unique. */
    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    /** For vertices that represent stops, the passenger-facing stop ID 
     *  (for systems like TriMet that have this feature).  */
    public AgencyAndId getStopId() {
        return stopId;
    }

    /** Get this vertex's unique index, that can serve as a hashcode or an index into a table */
    public int getIndex() {
        return index;
    }
    
    /** Get the highest unique index that has been assigned to a vertex.
     *  Used for making tables big enough to contain all existing vertices. */
    public static int getMaxIndex() {
        return maxIndex;
    }
    
    
    /* SERIALIZATION METHODS */
    
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

    
    /* UTILITY METHODS FOR GRAPH BUILDING AND GENERATING WALKSTEPS */
    
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
    
    /**
     * Clear this vertex's outgoing and incoming edge lists, and remove all the edges
     * they contained from this vertex's neighbors.
     */
    public void removeAllEdges() {
        for (Edge e : outgoing) {
            if (e instanceof DirectEdge) {
                DirectEdge edge = (DirectEdge) e;
                // this used to grab the GraphVertex by label... now it could possibly be a vertex
                // that is not in the graph
                // Vertex target = vertices.get(edge.getToVertex().getLabel());
                Vertex target = edge.getToVertex();
                if (target != null) {
                    target.removeIncoming(e);
                }
            }
        }
        for (Edge e : incoming) {
            // why only DirectEdges?
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
    
}