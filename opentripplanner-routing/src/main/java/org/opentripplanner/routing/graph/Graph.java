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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.MortonVertexComparator;
import org.opentripplanner.routing.core.TransferTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * A graph is really just one or more indexes into a set of vertexes. It used to keep edgelists for
 * each vertex, but those are in the vertex now.
 */
public class Graph implements Serializable, Iterable<Vertex> {
    // update serialVersionId to the current date in format YYYYMMDDL
    // whenever changes are made that could make existing graphs incompatible
    private static final long serialVersionUID = 20120121L;

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);
    
    // transit feed validity information in seconds since epoch
    private long transitServiceStarts = Long.MAX_VALUE;

    private long transitServiceEnds = 0;

    private Map<Class<?>, Object> _services = new HashMap<Class<?>, Object>();

    private TransferTable transferTable = new TransferTable();

    private GraphBundle bundle;
    
    /* vertex index by name is reconstructed from edges */
    private transient Set<Vertex> vertices;

    private transient ContractionHierarchySet hierarchies;
    
    private transient List<Vertex> vertexById;

    private transient Map<Integer, Edge> edgeById;
    
    private transient Map<Edge, Integer> idForEdge;
    
    private List<GraphBuilderAnnotation> graphBuilderAnnotations = new LinkedList<GraphBuilderAnnotation>();

    public Graph(Graph basedOn) {
        this();
        this.bundle = basedOn.getBundle();
    }

    public Graph() {
        this.vertices = new HashSet<Vertex>();
    }

    /**
     * Add the given vertex to the graph.
     * Ideally, only vertices should add themselves to the graph, 
     * when they are constructed or deserialized.
     * 
     * @param vv the vertex to add
     */
    protected void addVertex(Vertex v) {
        if (! vertices.add(v))
            LOG.warn("repeatedly added the same vertex: {}", v);
    }

    // called from streetutils, must be public for now
    // could makeEdgeBased be moved into Graph?
    public void removeVertex(Vertex v) {
        if (! vertices.remove(v))
            LOG.warn("attempting to remove vertex that is not in graph: {}", v);
    }

    public Set<Vertex> getVertices() {
        return this.vertices;
    }

    public boolean contains(Vertex v) {
        return vertices.contains(v);
    }

    public Vertex nearestVertex(float lat, float lon) {
        double minDist = Float.MAX_VALUE;
        Vertex ret = null;
        Coordinate c = new Coordinate(lon, lat);
        for (Vertex vv : this) {
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

    public void remove(Vertex vertex) {
        vertices.remove(vertex);
    }

    public void removeVertexAndEdges(Vertex vertex) {
        if (! vertices.contains(vertex)) {
            throw new IllegalStateException(
                    "attempting to remove vertex that is not in graph.");
        }
        vertex.removeAllEdges();
        vertices.remove(vertex);
    }

    public Envelope getExtent() {
        Envelope env = new Envelope();
        for (Vertex v : this) {
            env.expandToInclude(v.getCoordinate());
        }
        return env;
    }

    public TransferTable getTransferTable() {
        return transferTable;
    }

    // Infer the time period covered by the transit feed
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
        for (Vertex v : this) {
            ne += v.getDegreeOut();
        }
        return ne;
    }
    
    /* this will require a rehash of any existing hashtables keyed on vertices */
    private void renumberVerticesAndEdges() {
        this.vertexById = new ArrayList<Vertex>(vertices);
        Collections.sort(this.vertexById, new MortonVertexComparator(this.vertexById));
        this.edgeById = new HashMap<Integer, Edge>();
        this.idForEdge = new HashMap<Edge, Integer>();
        int i = 0;
        for (Vertex v : this.vertexById) {
            v.setIndex(i);
            int j = 0;
            for (Edge e : v.getOutgoing()) {
                int eid = (i*100) + j;
                // check for non-null?
                this.edgeById.put(eid, e);
                this.idForEdge.put(e, eid);
                ++j;
            }
            ++i;
        }
    }
    
    public void addBuilderAnnotation(GraphBuilderAnnotation gba) {
    	this.graphBuilderAnnotations.add(gba);
    }

    public List<GraphBuilderAnnotation> getBuilderAnnotations() {
    	return this.graphBuilderAnnotations;
    }

    public void setHierarchies(ContractionHierarchySet chs) {
        this.hierarchies = chs;
    }

    public ContractionHierarchySet getHierarchies() {
        return hierarchies;
    }

    /* (de) serialization */
    
    public enum LoadLevel {
        BASIC, FULL, NO_HIERARCHIES, DEBUG;
    }
    
    public static Graph load(File file, LoadLevel level) 
        throws IOException, ClassNotFoundException {
        LOG.info("Reading graph " + file.getAbsolutePath() + " ...");
        // cannot use getClassLoader() in static context
        ObjectInputStream in = new ObjectInputStream (new FileInputStream(file));
        return load(in, level);
    }
    
    public static Graph load(ClassLoader classLoader, File file, LoadLevel level) 
        throws IOException, ClassNotFoundException {
        LOG.info("Reading graph " + file.getAbsolutePath() + " with alternate classloader ...");
        ObjectInputStream in = new GraphObjectInputStream(
                new BufferedInputStream (new FileInputStream(file)), classLoader);
        return load(in, level);
    }

    @SuppressWarnings("unchecked")
    private static Graph load(ObjectInputStream in, LoadLevel level) 
        throws IOException, ClassNotFoundException {
        try {
            Graph graph = (Graph) in.readObject();
            LOG.debug("Basic graph info and annotations read.");
            if (level == LoadLevel.BASIC)
                return graph;
            // vertex edge lists are transient to avoid excessive recursion depth
            // vertex list is transient because it can be reconstructed from edges
            LOG.debug("Loading edges...");
            List<Edge> edges = (ArrayList<Edge>) in.readObject();
            graph.vertices = new HashSet<Vertex>();;
            for (Edge e : edges) {
               graph.vertices.add(e.getFromVertex());
               graph.vertices.add(e.getToVertex());
            }
            // trim edge lists to length
            for (Vertex v : graph)
                v.compact();
            LOG.info("Main graph read. |V|={} |E|={}", graph.countVertices(), graph.countEdges());
            if (level == LoadLevel.NO_HIERARCHIES)
                return graph;
            graph.hierarchies = (ContractionHierarchySet) in.readObject();
            if (graph.hierarchies != null)
                LOG.debug("Contraction hierarchies read.");
            if (level == LoadLevel.FULL)
                return graph;
            graph.vertexById = (List<Vertex>) in.readObject();
            graph.edgeById = (Map<Integer, Edge>) in.readObject();
            graph.idForEdge = (Map<Edge, Integer>) in.readObject();
            LOG.debug("Debug info read.");
            return graph;
        } catch (InvalidClassException ex) {
            LOG.error("Stored graph is incompatible with this version of OTP, please rebuild it.");
            throw new IllegalStateException("Stored Graph version error", ex);
        }
    }
    
    public void save(File file) throws IOException {
        if (!file.getParentFile().exists())
            if (!file.getParentFile().mkdirs())
                LOG.error("Failed to create directories for graph bundle at " + file);
        LOG.info("Main graph size: |V|={} |E|={}", this.countVertices(), this.countEdges());
        LOG.info("Writing graph " + file.getAbsolutePath() + " ...");
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)));
        LOG.debug("Consolidating edges...");
        // this is not space efficient
        List<Edge> edges = new ArrayList<Edge>(this.countEdges());
        for (Vertex v : this) {
            // there are assumed to be no edges in an incoming list that are not in an outgoing list
            edges.addAll(v.getOutgoing());
        }
        LOG.debug("Assigning vertex/edge ID numbers...");
        this.renumberVerticesAndEdges();
        LOG.debug("Writing edges...");
        out.writeObject(this);
        out.writeObject(edges);
        out.writeObject(this.hierarchies);
        LOG.debug("Writing debug data...");
        out.writeObject(this.vertexById);
        out.writeObject(this.edgeById);
        out.writeObject(this.idForEdge);
        out.close();
        LOG.info("Graph written.");
    }
    
    /* deserialization for org.opentripplanner.customize */
    private static class GraphObjectInputStream extends ObjectInputStream {
        ClassLoader classLoader;
        public GraphObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }
        @Override
        public Class<?> resolveClass(ObjectStreamClass osc) {
            try {
                return Class.forName(osc.getName(), false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Integer getIdForEdge(Edge edge) {
        return idForEdge.get(edge);
    }

    public Edge getEdgeById(int id) {
        return edgeById.get(id);
    }

    @Override
    public Iterator<Vertex> iterator() {
        return vertices.iterator();
    }

}