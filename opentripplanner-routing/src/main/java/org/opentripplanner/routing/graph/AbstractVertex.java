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

package org.opentripplanner.routing.graph;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.geometry.Pointlike;

import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.graph.Edge;
import javax.xml.bind.annotation.XmlTransient;

import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVertex implements Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVertex.class);

    private static int maxIndex = 0;

    private int index;
    
    private transient int groupIndex = -1;

    /* short debugging name */
    private final String label;
    
    /* Longer human-readable name for the client */
    private String name;

    private final double x;

    private final double y;
    
    private double distanceToNearestTransitStop = 0;

    private transient Set<Edge> incoming = new CopyOnWriteArraySet<Edge>();

    private transient Set<Edge> outgoing = new CopyOnWriteArraySet<Edge>();

    
    /* PUBLIC CONSTRUCTORS */
    
    public AbstractVertex(Graph g, String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.index = maxIndex  ++;
        // null graph means temporary vertex
        if (g != null)
            g.addVertex(this);
        this.name = "(no name provided)";
    }

    protected AbstractVertex(Graph g, String label, double x, double y, String name) {
        this(g, label, x, y);
        this.name = name;
    }

    
    /* PUBLIC METHODS */
    
    /** Distance in meters to the coordinate */
    @Override
    public double distance(Coordinate c) {
        return DistanceLibrary.distance(getY(), getX(), c.y, c.x);
    }

    /** Distance in meters to the vertex */
    @Override
    public double distance(Pointlike p) {
        return DistanceLibrary.distance(getLat(), getLon(), p.getLat(), p.getLon());
    }
    
    /** Fast, slightly approximated, under-estimated distance in meters to the vertex */
    @Override
    public double fastDistance(Pointlike p) {
        return DistanceLibrary.fastDistance(getLat(), getLon(), p.getLat(), p.getLon());
    }

    @Override
    public String toString() {
        return "<" + this.label + ">";
    }

    public int hashCode() {
        return index;
    }


    /* FIELD ACCESSOR METHODS : READ/WRITE */

    @Override
    public void addOutgoing(Edge ee) {
        if (outgoing.contains(ee)) {
            LOG.error("repeatedly added edge {} to vertex {}", ee, this);
        } else {
            outgoing.add(ee);
        }
    }
    
    @Override
    public void removeOutgoing(Edge ee) {
        outgoing.remove(ee);
        if (outgoing.contains(ee)) {
            LOG.error("edge {} still in edgelist of {} after removed. there must have been multiple copies.");
        }
    }

    /** Get a collection containing all the edges leading from this vertex to other vertices. */
    @Override
    public Collection<Edge> getOutgoing() {
        return outgoing;
    }

    @Override
    public void addIncoming(Edge ee) {
        if (incoming.contains(ee)) {
            LOG.error("repeatedly added edge {} to vertex {}", ee, this);
        } else {        
            incoming.add(ee);
        }
    }
    
    @Override
    public void removeIncoming(Edge ee) {
        incoming.remove(ee);
        if (incoming.contains(ee)) {
            LOG.error("edge {} still in edgelist of {} after removed. there must have been multiple copies.");
        }
    }

    /** Get a collection containing all the edges leading from other vertices to this vertex. */
    @Override
    public Collection<Edge> getIncoming() {
        return incoming;
    }

    @Override
    @XmlTransient
    public int getDegreeOut() {
        return outgoing.size();
    }

    @Override
    @XmlTransient
    public int getDegreeIn() {
        return incoming.size();
    }
    
    @Override
    public void setDistanceToNearestTransitStop(double distance) {
        distanceToNearestTransitStop = distance;
    }
    
    @Override
    public double getDistanceToNearestTransitStop() {
        return distanceToNearestTransitStop;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getLon() {
        return x;
    }

    @Override
    public double getLat() {
        return y;
    }

    @Override
    public void setGroupIndex(int groupIndex) {
        this.groupIndex = groupIndex;
    }
    
    @Override
    @XmlTransient
    public int getGroupIndex() {
        return groupIndex;
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setStreetName(String name) {
        this.name = name;
    }


    /* FIELD ACCESSOR METHODS : READ ONLY */

    @Override
    public String getLabel() {
        return label;
    }

    @XmlTransient
    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    /** Get this vertex's unique index, that can serve as a hashcode or an index into a table */
    @XmlTransient
    public int getIndex() {
        return index;
    }
    
    @Override
    public void setIndex(int index) {
        this.index = index;
    }
    
    public static int getMaxIndex() {
        return maxIndex;
    }
    
    
    /* SERIALIZATION METHODS */
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        // edge lists are transient 
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.incoming = new CopyOnWriteArraySet<Edge>();
        this.outgoing = new CopyOnWriteArraySet<Edge>();
        index = maxIndex++;
    }

    @Override
    public void compact() {
// copy-on-write array list never has extra empty slots
//        this.outgoing.trimToSize();
//        this.incoming.trimToSize();
    }
    

    /* UTILITY METHODS FOR SEARCHING, GRAPH BUILDING, AND GENERATING WALKSTEPS */
    
    @Override
    @XmlTransient
    public List<Edge> getOutgoingStreetEdges() {
        List<Edge> result = new ArrayList<Edge>();
        for (Edge out : this.getOutgoing()) {
            if (!(out instanceof TurnEdge || out instanceof OutEdge || out instanceof PlainStreetEdge)) {
                continue;
            }
            result.add((StreetEdge) out);
        }
        return result;
    }

    @Override
    public void mergeFrom(Graph graph, Vertex other) {
        // copy edgelists to avoid concurrent modification
        List<Edge> edges = new ArrayList<Edge>();
        edges.addAll(this.getIncoming());
        edges.addAll(this.getOutgoing());
        edges.addAll(other.getIncoming());
        edges.addAll(other.getOutgoing());

        for (Edge e : edges) {
            // We only support Vertices that are direct edges when merging
            Vertex from = e.getFromVertex();
            Vertex to = e.getToVertex();
            if ((from==this && to==other) || (from==other && to==this)) {
                e.detach();
            } else if (from == other) {
                e.attach(this, to);
            } else if (to == other) {
                e.attach(from, this);
            }
        }
        graph.removeVertex(other);
    }
    
    @Override
    public void removeAllEdges() {
        for (Edge e : outgoing) {
            Vertex target = e.getToVertex();
            if (target != null) {
                target.removeIncoming(e);
            }
        }
        for (Edge e : incoming) {
            Vertex source = e.getFromVertex();
            if (source != null) {
                source.removeOutgoing(e);
            }
        }
        incoming = new CopyOnWriteArraySet<Edge>();
        outgoing = new CopyOnWriteArraySet<Edge>();
    }
    
    @Override
    @XmlTransient
    public Collection<Edge> getEdges(OverlayGraph extraEdges, OverlayGraph replacementEdges, boolean incoming) {
        Collection<Edge> ret;
        if (replacementEdges != null) {
            ret = incoming ? replacementEdges.getIncoming(this) : replacementEdges.getOutgoing(this);
        } else {
            ret = incoming ? this.getIncoming() : this.getOutgoing();
        }
        if (extraEdges != null) {
            Collection<Edge> extra = incoming ? extraEdges.getIncoming(this) : extraEdges.getOutgoing(this);
            if (extra != null) {
                ret = new ArrayList<Edge>(ret); // copy list
                ret.addAll(extra);
                return ret;
            }
        }
        return ret;
    }

    
    /* GRAPH COHERENCY AND TYPE CHECKING */
   
    // Parameterized Class<? extends Edge) gets ugly fast here
    @SuppressWarnings("unchecked")
    private static final ValidEdgeTypes VALID_EDGE_TYPES = new ValidEdgeTypes(Edge.class);
    
    @XmlTransient
    @Override
    public ValidEdgeTypes getValidOutgoingEdgeTypes() {
        return VALID_EDGE_TYPES;
    }

    @XmlTransient
    @Override
    public ValidEdgeTypes getValidIncomingEdgeTypes() {
        return VALID_EDGE_TYPES ;
    }

    /*
     * This may not be necessary if edge constructor types are strictly specified
     * and addOutgoing is protected
     */
    @Override
    public boolean edgeTypesValid() {
        ValidEdgeTypes validOutgoingTypes = getValidOutgoingEdgeTypes();
        for (Edge e : getOutgoing())
            if (!validOutgoingTypes.isValid(e))
                return false;
        ValidEdgeTypes validIncomingTypes = getValidIncomingEdgeTypes();
        for (Edge e : getIncoming())
            if (!validIncomingTypes.isValid(e))
                return false;
        return true;
    }
    
    public static final class ValidEdgeTypes {
        private final Class<? extends Edge>[] classes;
        // varargs constructor: 
        // a loophole in the law against arrays/collections of parameterized generics
        public ValidEdgeTypes (Class<? extends Edge>... classes) {
            this.classes = classes;
        }
        public boolean isValid (Edge e) {
            for (Class<? extends Edge> c : classes) {
                if (c.isInstance(e)) 
                    return true;
            }
            return false;
        }
    }

}