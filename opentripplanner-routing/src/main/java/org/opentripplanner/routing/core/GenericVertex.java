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

import static org.opentripplanner.common.IterableLibrary.cast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class GenericVertex implements Vertex, Serializable {

    private static final long serialVersionUID = 364261663335739528L;

    public final String label;
    
    protected String name;

    private AgencyAndId stopId = null;

    private double y;

    private double x;

    int index;

    private double distanceToNearestTransitStop = 0;

    static int maxIndex = 0;
    
    /* --- ex-graphvertex ---*/
    
    ArrayList<Edge> incoming = new ArrayList<Edge>();
    ArrayList<Edge> outgoing = new ArrayList<Edge>();

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
    
    @Override
    public int getDegreeIn() {
        return incoming.size();
    }

    @Override
    public int getDegreeOut() {
        return outgoing.size();
    }

    @Override
    public Collection<Edge> getIncoming() {
        return incoming;
    }

    @Override
    public Collection<Edge> getOutgoing() {
        return outgoing;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        incoming.trimToSize();
        outgoing.trimToSize();
        out.defaultWriteObject();
    }

    /* --- END ex-graphvertex ---*/
    
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
    
    public GenericVertex(String label, double x, double y, String name, AgencyAndId stopId) {
        this(label, x, y);
        this.name = name;
        this.stopId = stopId;
    }

    public double distance(Coordinate c) {
        return DistanceLibrary.distance(getY(), getX(), c.y, c.x);
    }

    @Override
    public double distance(Vertex v) {
        return DistanceLibrary.distance(getY(), getX(), v.getY(), v.getX());
    }
    
    @Override
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

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AgencyAndId getStopId() {
        return stopId;
    }

    /**
     * Merge another vertex into this one.  Useful during graph construction for handling 
     * sequential non-branching streets, and empty dwells.
     */
    public void mergeFrom(Graph graph, GenericVertex other) {
        GraphVertex gv = graph.getGraphVertex(this);

        // We only support Vertices that are direct edges when merging
        Iterable<DirectEdge> incoming = cast(gv.incoming);
        Iterable<DirectEdge> outgoing = cast(gv.outgoing);
        Iterator<DirectEdge> it = incoming.iterator();
        //remove incoming edges from other to this
        while(it.hasNext()) {
            DirectEdge edge = it.next();
            if (edge.getFromVertex() == other) {
                it.remove();
            }
        }
        //remove outgoing edges from other to this
        it = outgoing.iterator();
        while(it.hasNext()) {
            DirectEdge edge = it.next();
            if (edge.getToVertex() == other) {
                it.remove();
            }
        }
        //make incoming edges to other point to this
        for (AbstractEdge edge : cast(graph.getIncoming(other), AbstractEdge.class)) {
            if (edge.getFromVertex() == this) {
                continue;
            }

            edge.setToVertex(this);
            gv.addIncoming(edge);
        }
        //add outgoing edges from other to outgoing from this
        for (AbstractEdge edge : cast(graph.getOutgoing(other), AbstractEdge.class)) {
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
}
