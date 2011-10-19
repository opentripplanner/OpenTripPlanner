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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.impl.ContractionPathServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * A graph is really just one or more indexes into a set of vertexes. It used to keep edgelists for
 * each vertex, but those are in the vertex now.
 */
public class Graph implements Serializable {
    // update serialVersionId to the current date in format YYYYMMDDL
    // whenever changes are made that could make existing graphs incompatible
    private static final long serialVersionUID = 20111019L;

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);
    
    // transit feed validity information in seconds since epoch
    private long transitServiceStarts = Long.MAX_VALUE;

    private long transitServiceEnds = 0;

    private Map<Class<?>, Object> _services = new HashMap<Class<?>, Object>();

    private transient HashMap<String, Vertex> vertices = new HashMap<String, Vertex>();

    private TransferTable transferTable = new TransferTable();

    private GraphBundle bundle;

    public Graph(Graph basedOn) {
        this();
        this.bundle = basedOn.getBundle();
    }

    public Graph() {
        this.vertices = new HashMap<String, Vertex>();
    }

    /**
     * Add the given vertex to the graph, or if one already exists with the same label, return that
     * instead.
     * 
     * @param vv the vertex to add
     * @return the existing or new vertex
     */
    public Vertex addVertex(Vertex vv) {
        String label = vv.getLabel();
        Vertex gv = vertices.get(label);
        if (gv == null) {
            vertices.put(label, vv);
            return vv;
        }
        return vv;
    }

    /**
     * If the graph contains a vertex with the given label, return it. Otherwise, create a new
     * GenericVertex with the given label and coordinates, add it to the graph, and return it.
     */
    public Vertex addVertex(String label, double x, double y) {
        Vertex gv = vertices.get(label);
        if (gv == null) {
            Vertex vv = new GenericVertex(label, x, y);
            vertices.put(label, vv);
            return vv;
        }
        return gv;
    }

    /**
     * If the graph contains a vertex with the given label, return it. Otherwise, create a new
     * GenericVertex with the given parameters, add it to the graph, and return it.
     */
    public Vertex addVertex(String label, String name, AgencyAndId stopId, double x, double y) {
        Vertex gv = vertices.get(label);
        if (gv == null) {
            Vertex vv = new GenericVertex(label, x, y, name, stopId);
            vertices.put(label, vv);
            return vv;
        }
        return gv;
    }

    /**
     * If the graph contains a vertex with the given label, return it. Otherwise, return null.
     */
    public Vertex getVertex(String label) {
        return vertices.get(label);
    }

    // DEPRECATED
    // public GraphVertex getGraphVertex(String label) {
    // return vertices.get(label);
    // }

    public Collection<Vertex> getVertices() {
        return vertices.values();
    }

    public void addEdge(Vertex a, Vertex b, Edge ee) {
        a = addVertex(a);
        b = addVertex(b);
        // there is the potential here for the edge lists to no longer match
        // the vertices pointed to by the edge
        if (ee.getFromVertex() != a)
            throw new IllegalStateException("Saving an edge with the wrong vertex.");
        a.addOutgoing(ee);
        b.addIncoming(ee);
    }

    public void addEdge(DirectEdge ee) {
        Vertex fromv = ee.getFromVertex();
        Vertex tov = ee.getToVertex();
        fromv = addVertex(fromv);
        tov = addVertex(tov);
        // there is the potential here for the edge lists to no longer match
        // the vertices pointed to by the edge
        if (ee.getFromVertex() != fromv || ee.getToVertex() != tov)
            throw new IllegalStateException("Saving an edge with the wrong vertex.");
        fromv.addOutgoing(ee);
        tov.addIncoming(ee);
    }

    public void addEdge(String from_label, String to_label, Edge ee) {
        Vertex a = this.getVertex(from_label);
        Vertex b = this.getVertex(to_label);
        // there is the potential here for the edge lists to no longer match
        // the vertices pointed to by the edge
        if (ee.getFromVertex() != a)
            throw new IllegalStateException("Saving an edge with the wrong vertex.");
        addEdge(a, b, ee);
    }

    public Vertex nearestVertex(float lat, float lon) {
        double minDist = Float.MAX_VALUE;
        Vertex ret = null;
        Coordinate c = new Coordinate(lon, lat);
        for (Vertex vv : vertices.values()) {
            double dist = vv.distance(c);
            if (dist < minDist) {
                ret = vv;
                minDist = dist;
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <T> T putService(Class<T> serviceType, T service) {
        return (T) _services.put(serviceType, service);
    }

    public boolean hasService(Class<?> serviceType) {
        return _services.containsKey(serviceType);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceType) {
        return (T) _services.get(serviceType);
    }

    public void removeVertex(Vertex vertex) {
        vertices.remove(vertex.getLabel());
    }

    public void removeVertexAndEdges(Vertex vertex) {
        Vertex gv = this.getVertex(vertex.getLabel());
        if (gv == null) {
            return;
        }
        if (gv != vertex) {
            throw new IllegalStateException(
                    "Vertex has the same label as one in the graph, but is not the same object.");
        }
        vertex.removeAllEdges();
        vertices.remove(vertex.getLabel());
    }

    public void removeEdge(AbstractEdge e) {
        Vertex fv = e.getFromVertex();
        Vertex fgv = vertices.get(fv.getLabel());
        if (fgv != null) {
            if (fv != fgv)
                throw new IllegalStateException("Vertex / edge mismatch.");
            else
                fgv.removeOutgoing(e);
        }
        Vertex tv = e.getToVertex();
        Vertex tgv = vertices.get(tv.getLabel());
        if (tgv != null) {
            if (tv != tgv)
                throw new IllegalStateException("Vertex / edge mismatch.");
            else
                tgv.removeIncoming(e);
        }
    }

    public Envelope getExtent() {
        Envelope env = new Envelope();
        for (Vertex v : this.getVertices()) {
            env.expandToInclude(v.getCoordinate());
        }
        return env;
    }

// DEPRECATED
//
//    public Collection<Edge> getOutgoing(Vertex v) {
//        return v.getOutgoing();
//    }
//
//    public Collection<Edge> getIncoming(Vertex v) {
//        return v.getIncoming();
//    }
//
//    public int getDegreeOut(Vertex v) {
//        return v.getDegreeOut();
//    }
//
//    public int getDegreeIn(Vertex v) {
//        return v.getDegreeIn();
//    }
//
//    public Collection<Edge> getIncoming(String label) {
//        return vertices.get(label).getIncoming();
//    }
//
//    public Collection<Edge> getOutgoing(String label) {
//        return vertices.get(label).getOutgoing();
//    }

    // DEPRECATED
    // public GraphVertex getGraphVertex(Vertex vertex) {
    // return getGraphVertex(vertex.getLabel());
    // }

    // DEPRECATED
    // public void addGraphVertex(GraphVertex graphVertex) {
    // vertices.put(graphVertex.vertex.getLabel(), graphVertex);
    // }

    public TransferTable getTransferTable() {
        return transferTable;
    }

    // Infer the time period covered by the trasit feed
    public void updateTransitFeedValidity(CalendarServiceData data) {
        final long SEC_IN_DAY = 24 * 60 * 60;
        for (AgencyAndId sid : data.getServiceIds()) {
            for (ServiceDate sd : data.getServiceDatesForServiceId(sid)) {
                long t = sd.getAsDate().getTime();
                // assume feed is unreliable after midnight on last service day
                long u = t + SEC_IN_DAY;
                if (t < this.transitServiceStarts)
                    this.transitServiceStarts = t;
                if (u > this.transitServiceEnds)
                    this.transitServiceEnds = u;
            }
        }
    }

    // Check to see if we have transit information for a given date
    public boolean transitFeedCovers(Date d) {
        long t = d.getTime();
        return t >= this.transitServiceStarts && t < this.transitServiceEnds;
    }

    public GraphBundle getBundle() {
        return bundle;
    }

    public void setBundle(GraphBundle bundle) {
        this.bundle = bundle;
    }    
    
    public int countVertices() {
        return vertices.size();
    }

    /**
     * Find the total number of edges in this Graph. There are assumed to be no Edges 
     * in an incoming edge list that are not in an outgoing edge list. 
     * @return number of outgoing edges in the graph
     */
    public int countEdges() {
        int ne = 0;
        for (Vertex v : vertices.values()) {
            ne += v.getDegreeOut();
        }
        return ne;
    }
    
    /* (de) serialization */
    
    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        LOG.debug("Consolidating edges...");
        List<Edge> edges = new ArrayList<Edge>(countVertices() * 2);
        for (Vertex v : vertices.values()) {
            // there are assumed to be no edges in an incoming list that are not in an outgoing list
            edges.addAll(v.getOutgoing());
        }
        LOG.debug("Writing graph...");
        out.defaultWriteObject();
        out.writeObject(edges);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        LOG.debug("Reading graph...");
        in.defaultReadObject();
        List<Edge> edges = (ArrayList<Edge>) in.readObject();
        LOG.debug("Loading edges...");
        this.vertices = new HashMap<String, Vertex>();
        int count = 0;
        for (Edge e : edges) {
            if (++count % 100000 == 0)
                LOG.debug("loading edge {} / {}", count, edges.size());
            Vertex fromv = e.getFromVertex();
            Vertex tov = null;
            if (e instanceof AbstractEdge)
                tov = ((AbstractEdge)e).getToVertex();
            else if (e instanceof TurnEdge)
                tov = ((TurnEdge)e).getToVertex();
            else
                LOG.warn("Edge with no to-vertex: " + e);
            
            vertices.put(fromv.getLabel(), fromv);
            fromv.addOutgoing(e);
            if (tov != null) {
                vertices.put(tov.getLabel(), tov);
                tov.addIncoming(e);
            }
        }
    }

}