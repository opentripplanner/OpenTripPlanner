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


import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.linked.TDoubleLinkedList;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.joda.time.DateTime;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.GraphUtils;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.NoFutureDates;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.MortonVertexComparatorFactory;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.EdgeWithCleanup;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.traffic.StreetSpeedSnapshotSource;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.WorldEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
/**
 * A graph is really just one or more indexes into a set of vertexes. It used to keep edgelists for each vertex, but those are in the vertex now.
 */
public class Graph implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private final MavenVersion mavenVersion = MavenVersion.VERSION;

    // TODO Remove this field, use Router.routerId ?
    public String routerId;

    private final Map<Edge, Set<AlertPatch>> alertPatches = new HashMap<Edge, Set<AlertPatch>>(0);

    private final Map<Edge, List<TurnRestriction>> turnRestrictions = Maps.newHashMap();

    public final StreetNotesService streetNotesService = new StreetNotesService();

    // transit feed validity information in seconds since epoch
    private long transitServiceStarts = Long.MAX_VALUE;

    private long transitServiceEnds = 0;

    private Map<Class<?>, Object> _services = new HashMap<Class<?>, Object>();

    private TransferTable transferTable = new TransferTable();

    private GraphBundle bundle;

    /* vertex index by name is reconstructed from edges */
    private transient Map<String, Vertex> vertices;

    private transient CalendarService calendarService;

    private boolean debugData = true;

    // TODO this would be more efficient if it was just an array.
    private transient Map<Integer, Vertex> vertexById;

    private transient Map<Integer, Edge> edgeById;

    public transient StreetVertexIndexService streetIndex;

    public transient GraphIndex index;

    private transient GeometryIndex geomIndex;

    private transient SampleFactory sampleFactory;

    public final Deduplicator deduplicator = new Deduplicator();

    /**
     * Map from GTFS ServiceIds to integers close to 0. Allows using BitSets instead of Set<Object>.
     * An empty Map is created before the Graph is built to allow registering IDs from multiple feeds.   
     */
    public final Map<AgencyAndId, Integer> serviceCodes = Maps.newHashMap();

    public transient TimetableSnapshotSource timetableSnapshotSource = null;

    private transient List<GraphBuilderAnnotation> graphBuilderAnnotations = new LinkedList<GraphBuilderAnnotation>(); // initialize for tests

    private Map<String, Collection<Agency>> agenciesForFeedId = new HashMap<>();

    private Collection<String> feedIds = new HashSet<>();

    private VertexComparatorFactory vertexComparatorFactory = new MortonVertexComparatorFactory();

    private transient TimeZone timeZone = null;

    //Envelope of all OSM and transit vertices. Calculated during build time
    private WorldEnvelope envelope = null;

    //ConvexHull of all the graph vertices. Generated at Graph build time.
    private Geometry convexHull = null;

    /** The density center of the graph for determining the initial geographic extent in the client. */
    private Coordinate center = null;

    /** The config JSON used to build this graph. Allows checking whether the configuration has changed. */
    public String builderConfig = null;

    /** Embed a router configuration inside the graph, for starting up with a single file. */
    public String routerConfig = null;

    /* The preferences that were used for graph building. */
    public Preferences preferences = null;

    /* The time at which the graph was built, for detecting changed inputs and triggering a rebuild. */
    public DateTime buildTimeJoda = null; // FIXME record this info, null is just a placeholder

    /** List of transit modes that are availible in GTFS data used in this graph**/
    private HashSet<TraverseMode> transitModes = new HashSet<TraverseMode>();

    public boolean hasBikeSharing = false;

    public boolean hasParkRide = false;

    public boolean hasBikeRide = false;

    /**
     * Manages all updaters of this graph. Is created by the GraphUpdaterConfigurator when there are
     * graph updaters defined in the configuration.
     *
     * @see GraphUpdaterConfigurator
     */
    public transient GraphUpdaterManager updaterManager = null;

    public final Date buildTime = new Date();

    /** True if OSM data was loaded into this Graph. */
    public boolean hasStreets = false;

    /** True if GTFS data was loaded into this Graph. */
    public boolean hasTransit = false;

    /** True if direct single-edge transfers were generated between transit stops in this Graph. */
    public boolean hasDirectTransfers = false;

    /** True if frequency-based services exist in this Graph (GTFS frequencies with exact_times = 0). */
    public boolean hasFrequencyService = false;

    /** True if schedule-based services exist in this Graph (including GTFS frequencies with exact_times = 1). */
    public boolean hasScheduledService = false;

    /** Has information how much time boarding a vehicle takes. Can be significant eg in airplanes or ferries. */
    public Map<TraverseMode, Integer> boardTimes = Collections.EMPTY_MAP;

    /** Has information how much time alighting a vehicle takes. Can be significant eg in airplanes or ferries. */
    public Map<TraverseMode, Integer> alightTimes = Collections.EMPTY_MAP;

    /** A speed source for traffic data */
    public transient StreetSpeedSnapshotSource streetSpeedSource;

    public Graph(Graph basedOn) {
        this();
        this.bundle = basedOn.getBundle();
    }

    public Graph() {
        this.vertices = new ConcurrentHashMap<String, Vertex>();
        this.edgeById = new ConcurrentHashMap<Integer, Edge>();
        this.vertexById = new ConcurrentHashMap<Integer, Vertex>();
    }

    /**
     * Add the given vertex to the graph. Ideally, only vertices should add themselves to the graph, when they are constructed or deserialized.
     */
    public void addVertex(Vertex v) {
        Vertex old = vertices.put(v.getLabel(), v);
        if (old != null) {
            if (old == v)
                LOG.error("repeatedly added the same vertex: {}", v);
            else
                LOG.error("duplicate vertex label in graph (added vertex to graph anyway): {}", v);
        }
    }

    /**
     * Removes a vertex from the graph.
     *
     * Called from streetutils, must be public for now
     *
     * @param v
     */
    public void removeVertex(Vertex v) {
        if (vertices.remove(v.getLabel()) != v) {
            LOG.error(
                    "attempting to remove vertex that is not in graph (or mapping value was null): {}",
                    v);
        }
    }

    /**
     * Removes an edge from the graph. This method is not thread-safe.
     * @param e The edge to be removed
     */
    public void removeEdge(Edge e) {
        if (e != null) {
            synchronized (alertPatches) {   // This synchronization is somewhat silly because this
                alertPatches.remove(e);     // method isn't thread-safe anyway, but it is consistent
            }

            turnRestrictions.remove(e);
            streetNotesService.removeStaticNotes(e);
            edgeById.remove(e.getId());

            if (e instanceof EdgeWithCleanup) ((EdgeWithCleanup) e).detach();

            if (e.fromv != null) {
                e.fromv.removeOutgoing(e);

                for (Edge otherEdge : e.fromv.getIncoming()) {
                    for (TurnRestriction turnRestriction : getTurnRestrictions(otherEdge)) {
                        if (turnRestriction.to == e) {
                            removeTurnRestriction(otherEdge, turnRestriction);
                        }
                    }
                }

                e.fromv = null;
            }

            if (e.tov != null) {
                e.tov.removeIncoming(e);
                e.tov = null;
            }
        }
    }

    /* Fetching vertices by label is convenient in tests and such, but avoid using in general. */
    @VisibleForTesting
    public Vertex getVertex(String label) {
        return vertices.get(label);
    }

    /**
     * Returns the vertex with the given ID or null if none is present.
     *
     * NOTE: you may need to run rebuildVertexAndEdgeIndices() for the indices
     * to be accurate.
     *
     * @param id
     * @return
     */
    public Vertex getVertexById(int id) {
        return this.vertexById.get(id);
    }

    /**
     * Get all the vertices in the graph.
     * @return
     */
    public Collection<Vertex> getVertices() {
        return this.vertices.values();
    }

    /**
     * Returns the edge with the given ID or null if none is present.
     *
     * NOTE: you may need to run rebuildVertexAndEdgeIndices() for the indices
     * to be accurate.
     *
     * @param id
     * @return
     */
    public Edge getEdgeById(int id) {
        return edgeById.get(id);
    }

    /**
     * Return all the edges in the graph.
     * @return
     */
    public Collection<Edge> getEdges() {
        Set<Edge> edges = new HashSet<Edge>();
        for (Vertex v : this.getVertices()) {
            edges.addAll(v.getOutgoing());
        }
        return edges;
    }

    /**
     * Add an {@link AlertPatch} to the {@link AlertPatch} {@link Set} belonging to an {@link Edge}.
     * @param edge
     * @param alertPatch
     */
    public void addAlertPatch(Edge edge, AlertPatch alertPatch) {
        if (edge == null || alertPatch == null) return;
        synchronized (alertPatches) {
            Set<AlertPatch> alertPatches = this.alertPatches.get(edge);
            if (alertPatches == null) {
                this.alertPatches.put(edge, Collections.singleton(alertPatch));
            } else if (alertPatches instanceof HashSet) {
                alertPatches.add(alertPatch);
            } else {
                alertPatches = new HashSet<AlertPatch>(alertPatches);
                if (alertPatches.add(alertPatch)) {
                    this.alertPatches.put(edge, alertPatches);
                }
            }
        }
    }

    /**
     * Remove an {@link AlertPatch} from the {@link AlertPatch} {@link Set} belonging to an
     * {@link Edge}.
     * @param edge
     * @param alertPatch
     */
    public void removeAlertPatch(Edge edge, AlertPatch alertPatch) {
        if (edge == null || alertPatch == null) return;
        synchronized (alertPatches) {
            Set<AlertPatch> alertPatches = this.alertPatches.get(edge);
            if (alertPatches != null && alertPatches.contains(alertPatch)) {
                if (alertPatches.size() < 2) {
                    this.alertPatches.remove(edge);
                } else {
                    alertPatches.remove(alertPatch);
                }
            }
        }
    }

    /**
     * Get the {@link AlertPatch} {@link Set} that belongs to an {@link Edge} and build a new array.
     * @param edge
     * @return The {@link AlertPatch} array that belongs to the {@link Edge}
     */
    public AlertPatch[] getAlertPatches(Edge edge) {
        if (edge != null) {
            synchronized (alertPatches) {
                Set<AlertPatch> alertPatches = this.alertPatches.get(edge);
                if (alertPatches != null) {
                    return alertPatches.toArray(new AlertPatch[alertPatches.size()]);
                }
            }
        }
        return new AlertPatch[0];
    }

    /**
     * Add a {@link TurnRestriction} to the {@link TurnRestriction} {@link List} belonging to an
     * {@link Edge}. This method is not thread-safe.
     * @param edge
     * @param turnRestriction
     */
    public void addTurnRestriction(Edge edge, TurnRestriction turnRestriction) {
        if (edge == null || turnRestriction == null) return;
        List<TurnRestriction> turnRestrictions = this.turnRestrictions.get(edge);
        if (turnRestrictions == null) {
            turnRestrictions = Lists.newArrayList();
            this.turnRestrictions.put(edge, turnRestrictions);
        }
        turnRestrictions.add(turnRestriction);
    }

    /**
     * Remove a {@link TurnRestriction} from the {@link TurnRestriction} {@link List} belonging to
     * an {@link Edge}. This method is not thread-safe.
     * @param edge
     * @param turnRestriction
     */
    public void removeTurnRestriction(Edge edge, TurnRestriction turnRestriction) {
        if (edge == null || turnRestriction == null) return;
        List<TurnRestriction> turnRestrictions = this.turnRestrictions.get(edge);
        if (turnRestrictions != null && turnRestrictions.contains(turnRestriction)) {
            if (turnRestrictions.size() < 2) {
                this.turnRestrictions.remove(edge);
            } else {
                turnRestrictions.remove(turnRestriction);
            }
        }
    }

    /**
     * Get the {@link TurnRestriction} {@link List} that belongs to an {@link Edge} and return an
     * immutable copy. This method is thread-safe when used by itself, but not if addTurnRestriction
     * or removeTurnRestriction is called concurrently.
     * @param edge
     * @return The {@link TurnRestriction} {@link List} that belongs to the {@link Edge}
     */
    public List<TurnRestriction> getTurnRestrictions(Edge edge) {
        if (edge != null) {
            List<TurnRestriction> turnRestrictions = this.turnRestrictions.get(edge);
            if (turnRestrictions != null) {
                return ImmutableList.copyOf(turnRestrictions);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Return only the StreetEdges in the graph.
     * @return
     */
    public Collection<StreetEdge> getStreetEdges() {
        Collection<Edge> allEdges = this.getEdges();
        return Lists.newArrayList(Iterables.filter(allEdges, StreetEdge.class));
    }    
    
    public boolean containsVertex(Vertex v) {
        return (v != null) && vertices.get(v.getLabel()) == v;
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

    public <T> T getService(Class<T> serviceType, boolean autoCreate) {
        @SuppressWarnings("unchecked")
        T t = (T) _services.get(serviceType);
        if (t == null && autoCreate) {
            try {
                t = (T)serviceType.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            _services.put(serviceType, t);
        }
        return t;
    }

    public void remove(Vertex vertex) {
        vertices.remove(vertex.getLabel());
    }

    public void removeVertexAndEdges(Vertex vertex) {
        if (!containsVertex(vertex)) {
            throw new IllegalStateException("attempting to remove vertex that is not in graph.");
        }

        /*
         * Note: We have to handle the removal of looping edges (for example RentABikeOn/OffEdge),
         * we use a set to prevent having multiple times the same edge.
         */
        Set<Edge> edges = new HashSet<Edge>(vertex.getDegreeIn() + vertex.getDegreeOut());
        edges.addAll(vertex.getIncoming());
        edges.addAll(vertex.getOutgoing());

        for (Edge edge : edges) {
            removeEdge(edge);
        }

        this.remove(vertex);
    }

    public Envelope getExtent() {
        Envelope env = new Envelope();
        for (Vertex v : getVertices()) {
            env.expandToInclude(v.getCoordinate());
        }
        return env;
    }

    public TransferTable getTransferTable() {
        return transferTable;
    }

    // Infer the time period covered by the transit feed
    public void updateTransitFeedValidity(CalendarServiceData data) {
        long now = new Date().getTime() / 1000;
        final long SEC_IN_DAY = 24 * 60 * 60;
        HashSet<String> agenciesWithFutureDates = new HashSet<String>();
        HashSet<String> agencies = new HashSet<String>();
        for (AgencyAndId sid : data.getServiceIds()) {
            agencies.add(sid.getAgencyId());
            for (ServiceDate sd : data.getServiceDatesForServiceId(sid)) {
                // Adjust for timezone, assuming there is only one per graph.
                long t = sd.getAsDate(getTimeZone()).getTime() / 1000;
                if (t > now) {
                    agenciesWithFutureDates.add(sid.getAgencyId());
                }
                // assume feed is unreliable after midnight on last service day
                long u = t + SEC_IN_DAY;
                if (t < this.transitServiceStarts)
                    this.transitServiceStarts = t;
                if (u > this.transitServiceEnds)
                    this.transitServiceEnds = u;
            }
        }
        for (String agency : agencies) {
            if (!agenciesWithFutureDates.contains(agency)) {
                LOG.warn(this.addBuilderAnnotation(new NoFutureDates(agency)));
            }
        }
    }

    // Check to see if we have transit information for a given date
    public boolean transitFeedCovers(long t) {
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
     * Find the total number of edges in this Graph. There are assumed to be no Edges in an incoming edge list that are not in an outgoing edge list.
     * 
     * @return number of outgoing edges in the graph
     */
    public int countEdges() {
        int ne = 0;
        for (Vertex v : getVertices()) {
            ne += v.getDegreeOut();
        }
        return ne;
    }

    /**
     * Add a collection of edges from the edgesById index.
     * @param es
     */
    private void addEdgesToIndex(Collection<Edge> es) {
        for (Edge e : es) {
            this.edgeById.put(e.getId(), e);
        }
    }
    
    /**
     * Rebuilds any indices on the basis of current vertex and edge IDs.
     * 
     * If you want the index to be accurate, you must run this every time the 
     * vertex or edge set changes.
     * 
     * TODO(flamholz): keep the indices up to date with changes to the graph.
     * This is not simple because the Vertex constructor may add itself to the graph
     * before the Vertex has any edges, so updating indices on addVertex is insufficient.
     */
    public void rebuildVertexAndEdgeIndices() {
        this.vertexById = new HashMap<Integer, Vertex>(Vertex.getMaxIndex());
        Collection<Vertex> vertices = getVertices();
        for (Vertex v : vertices) {
            vertexById.put(v.getIndex(), v);
        }

        // Create map from edge ids to edges.
        this.edgeById = new HashMap<Integer, Edge>();
        for (Vertex v : vertices) {
            // TODO(flamholz): this check seems superfluous.
            if (v == null) {
                continue;
            }

            // Assumes that all the edges appear in at least one outgoing edge list.
            addEdgesToIndex(v.getOutgoing());
        }
    }

    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException,
            IOException {
        inputStream.defaultReadObject();
    }

    /**
     * Add a graph builder annotation to this graph's list of graph builder annotations. The return value of this method is the annotation's message,
     * which allows for a single-line idiom that creates, registers, and logs a new graph builder annotation:
     * log.warning(graph.addBuilderAnnotation(new SomeKindOfAnnotation(param1, param2)));
     * 
     * If the graphBuilderAnnotations field of this graph is null, the annotation is not actually saved, but the message is still returned. This
     * allows annotation registration to be turned off, saving memory and disk space when the user is not interested in annotations.
     */
    public String addBuilderAnnotation(GraphBuilderAnnotation gba) {
        String ret = gba.getMessage();
        if (this.graphBuilderAnnotations != null)
            this.graphBuilderAnnotations.add(gba);
        return ret;
    }

    public List<GraphBuilderAnnotation> getBuilderAnnotations() {
        return this.graphBuilderAnnotations;
    }

    /**
     * Adds mode of transport to transit modes in graph
     * @param mode
     */
    public void addTransitMode(TraverseMode mode) {
        transitModes.add(mode);
    }

    public HashSet<TraverseMode> getTransitModes() {
        return transitModes;
    }

    /* (de) serialization */

    public enum LoadLevel {
        BASIC, FULL, DEBUG;
    }

    public static Graph load(File file, LoadLevel level) throws IOException, ClassNotFoundException {
        LOG.info("Reading graph " + file.getAbsolutePath() + " ...");
        // cannot use getClassLoader() in static context
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        return load(in, level);
    }

    public static Graph load(ClassLoader classLoader, File file, LoadLevel level)
            throws IOException, ClassNotFoundException {
        LOG.info("Reading graph " + file.getAbsolutePath() + " with alternate classloader ...");
        ObjectInputStream in = new GraphObjectInputStream(new BufferedInputStream(
                new FileInputStream(file)), classLoader);
        return load(in, level);
    }

    public static Graph load(InputStream is, LoadLevel level) throws ClassNotFoundException,
            IOException {
        return load(new ObjectInputStream(is), level);
    }

    /**
     * Default load. Uses DefaultStreetVertexIndexFactory.
     * @param in
     * @param level
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Graph load(ObjectInputStream in, LoadLevel level) throws IOException,
            ClassNotFoundException {
        return load(in, level, new DefaultStreetVertexIndexFactory());
    }
    
    /** 
     * Perform indexing on vertices, edges, and timetables, and create transient data structures.
     * This used to be done in readObject methods upon deserialization, but stand-alone mode now
     * allows passing graphs from graphbuilder to server in memory, without a round trip through
     * serialization. 
     * TODO: do we really need a factory for different street vertex indexes?
     */
    public void index(StreetVertexIndexFactory indexFactory) {
        streetIndex = indexFactory.newIndex(this);
        LOG.debug("street index built.");
        LOG.debug("Rebuilding edge and vertex indices.");
        rebuildVertexAndEdgeIndices();
        Set<TripPattern> tableTripPatterns = Sets.newHashSet();
        for (PatternArriveVertex pav : Iterables.filter(this.getVertices(), PatternArriveVertex.class)) {
            tableTripPatterns.add(pav.getTripPattern());
        }
        for (TripPattern ttp : tableTripPatterns) {
            if (ttp != null) ttp.scheduledTimetable.finish(); // skip frequency-based patterns with no table (null)
        }
        // TODO: Move this ^ stuff into the graph index
        this.index = new GraphIndex(this);
    }
    
    /**
     * Loading which allows you to specify StreetVertexIndexFactory and inject other implementation.
     * @param in
     * @param level
     * @param indexFactory
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static Graph load(ObjectInputStream in, LoadLevel level,
            StreetVertexIndexFactory indexFactory) throws IOException, ClassNotFoundException {
        try {
            Graph graph = (Graph) in.readObject();
            LOG.debug("Basic graph info read.");
            if (graph.graphVersionMismatch())
                throw new RuntimeException("Graph version mismatch detected.");
            if (level == LoadLevel.BASIC)
                return graph;
            // vertex edge lists are transient to avoid excessive recursion depth
            // vertex list is transient because it can be reconstructed from edges
            LOG.debug("Loading edges...");
            List<Edge> edges = (ArrayList<Edge>) in.readObject();
            graph.vertices = new HashMap<String, Vertex>();
            
            for (Edge e : edges) {
                graph.vertices.put(e.getFromVertex().getLabel(), e.getFromVertex());
                graph.vertices.put(e.getToVertex().getLabel(), e.getToVertex());
            }

            LOG.info("Main graph read. |V|={} |E|={}", graph.countVertices(), graph.countEdges());
            graph.index(indexFactory);

            if (level == LoadLevel.FULL) {
                return graph;
            }
            
            if (graph.debugData) {
                graph.graphBuilderAnnotations = (List<GraphBuilderAnnotation>) in.readObject();
                LOG.debug("Debug info read.");
            } else {
                LOG.warn("Graph file does not contain debug data.");
            }
            return graph;
        } catch (InvalidClassException ex) {
            LOG.error("Stored graph is incompatible with this version of OTP, please rebuild it.");
            throw new IllegalStateException("Stored Graph version error", ex);
        }
    }

    /**
     * Compares the OTP version number stored in the graph with that of the currently running instance. Logs warnings explaining that mismatched
     * versions can cause problems.
     * 
     * @return false if Maven versions match (even if commit ids do not match), true if Maven version of graph does not match this version of OTP or
     *         graphs are otherwise obviously incompatible.
     */
    private boolean graphVersionMismatch() {
        MavenVersion v = MavenVersion.VERSION;
        MavenVersion gv = this.mavenVersion;
        LOG.info("Graph version: {}", gv);
        LOG.info("OTP version:   {}", v);
        if (!v.equals(gv)) {
            LOG.error("This graph was built with a different version of OTP. Please rebuild it.");
            return true; // do not allow graph use
        } else if (!v.commit.equals(gv.commit)) {
            if (v.qualifier.equals("SNAPSHOT")) {
                LOG.warn("This graph was built with the same SNAPSHOT version of OTP, but a "
                        + "different commit. Please rebuild the graph if you experience incorrect "
                        + "behavior. ");
                return false; // graph might still work
            } else {
                LOG.error("Commit mismatch in non-SNAPSHOT version. This implies a problem with "
                        + "the build or release process.");
                return true; // major problem
            }
        } else {
            // no version mismatch, no commit mismatch
            LOG.info("This graph was built with the currently running version and commit of OTP.");
            return false;
        }
    }

    public void save(File file) throws IOException {
        LOG.info("Main graph size: |V|={} |E|={}", this.countVertices(), this.countEdges());
        LOG.info("Writing graph " + file.getAbsolutePath() + " ...");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(
                new FileOutputStream(file)));
        try {
            save(out);
            out.close();
        } catch (RuntimeException e) {
            out.close();
            file.delete(); // remove half-written file
            throw e;
        }
    }

    public void save(ObjectOutputStream out) throws IOException {
        LOG.debug("Consolidating edges...");
        // this is not space efficient
        List<Edge> edges = new ArrayList<Edge>(this.countEdges());
        for (Vertex v : getVertices()) {
            // there are assumed to be no edges in an incoming list that are not
            // in an outgoing list
            edges.addAll(v.getOutgoing());
            if (v.getDegreeOut() + v.getDegreeIn() == 0)
                LOG.debug("vertex {} has no edges, it will not survive serialization.", v);
        }
        LOG.debug("Assigning vertex/edge ID numbers...");
        this.rebuildVertexAndEdgeIndices();
        LOG.debug("Writing edges...");
        out.writeObject(this);
        out.writeObject(edges);
        if (debugData) {
            // should we make debug info generation conditional?
            LOG.debug("Writing debug data...");
            out.writeObject(this.graphBuilderAnnotations);
            out.writeObject(this.vertexById);
            out.writeObject(this.edgeById);
        } else {
            LOG.debug("Skipping debug data.");
        }
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
        return edge.getId();
    }

    public CalendarService getCalendarService() {
        if (calendarService == null) {
            CalendarServiceData data = this.getService(CalendarServiceData.class);
            if (data != null) {
                CalendarServiceImpl calendarService = new CalendarServiceImpl();
                calendarService.setData(data);
                this.calendarService = calendarService;
            }
        }
        return this.calendarService;
    }

    public int removeEdgelessVertices() {
        int removed = 0;
        List<Vertex> toRemove = new LinkedList<Vertex>();
        for (Vertex v : this.getVertices())
            if (v.getDegreeOut() + v.getDegreeIn() == 0)
                toRemove.add(v);
        // avoid concurrent vertex map modification
        for (Vertex v : toRemove) {
            this.remove(v);
            removed += 1;
            LOG.trace("removed edgeless vertex {}", v);
        }
        return removed;
    }

    public Collection<String> getFeedIds() {
        return feedIds;
    }

    public Collection<Agency> getAgencies(String feedId) {
        return agenciesForFeedId.get(feedId);
    }

    public void addAgency(String feedId, Agency agency) {
        Collection<Agency> agencies = agenciesForFeedId.getOrDefault(feedId, new HashSet<>());
        agencies.add(agency);
        this.agenciesForFeedId.put(feedId, agencies);
        this.feedIds.add(feedId);
    }

    /**
     * Returns the time zone for the first agency in this graph. This is used to interpret times in API requests. The JVM default time zone cannot be
     * used because we support multiple graphs on one server via the routerId. Ideally we would want to interpret times in the time zone of the
     * geographic location where the origin/destination vertex or board/alight event is located. This may become necessary when we start making graphs
     * with long distance train, boat, or air services.
     */
    public TimeZone getTimeZone() {
        if (timeZone == null) {
            Collection<Agency> agencies = null;
            if (agenciesForFeedId.entrySet().size() > 0) {
                agencies = agenciesForFeedId.entrySet().iterator().next().getValue();
            }
            if (agencies == null || agencies.size() == 0) {
                timeZone = TimeZone.getTimeZone("GMT");
                LOG.warn("graph contains no agencies (yet); API request times will be interpreted as GMT.");
            } else {
                CalendarService cs = this.getCalendarService();
                for (Agency agency : agencies) {
                    TimeZone tz = cs.getTimeZoneForAgencyId(agency.getId());
                    if (timeZone == null) {
                        LOG.debug("graph time zone set to {}", tz);
                        timeZone = tz;
                    } else if (!timeZone.equals(tz)) {
                        LOG.error("agency time zone differs from graph time zone: {}", tz);
                    }
                }
            }
        }
        return timeZone;
    }
    
    /**
     * The timezone is cached by the graph. If you've done something to the graph that has the
     * potential to change the time zone, you should call this to ensure it is reset. 
     */
    public void clearTimeZone () {
        this.timeZone = null;
    }

    public void summarizeBuilderAnnotations() {
        List<GraphBuilderAnnotation> gbas = this.graphBuilderAnnotations;
        Multiset<Class<? extends GraphBuilderAnnotation>> classes = HashMultiset.create();
        LOG.info("Summary (number of each type of annotation):");
        for (GraphBuilderAnnotation gba : gbas)
            classes.add(gba.getClass());
        for (Multiset.Entry<Class<? extends GraphBuilderAnnotation>> e : classes.entrySet()) {
            String name = e.getElement().getSimpleName();
            int count = e.getCount();
            LOG.info("    {} - {}", name, count);
        }
    }

    /**
     * Calculates envelope out of all OSM coordinates
     *
     * Transit stops are added to the envelope as they are added to the graph
     */
    public void calculateEnvelope() {
        this.envelope = new WorldEnvelope();

        for (Vertex v : this.getVertices()) {
            Coordinate c = v.getCoordinate();
            this.envelope.expandToInclude(c);
        }
    }

    /**
     * Calculates convexHull of all the vertices during build time
     */
    public void calculateConvexHull() {
        convexHull = GraphUtils.makeConvexHull(this);
    }

    /**
     * @return calculated convexHull;
     */
    public Geometry getConvexHull() {
        return convexHull;

    }

    /**
     * Expands envelope to include given point
     *
     * If envelope is empty it creates it (This can happen with a graph without OSM data)
     * Used when adding stops to OSM envelope
     *
     * @param  x  the value to lower the minimum x to or to raise the maximum x to
     * @param  y  the value to lower the minimum y to or to raise the maximum y to
     */
    public void expandToInclude(double x, double y) {
        //Envelope can be empty if graph building is run without OSM data
        if (this.envelope == null) {
            calculateEnvelope();
        }
        this.envelope.expandToInclude(x, y);
    }

    public WorldEnvelope getEnvelope() {
        return this.envelope;
    }

    // lazy-init geom index on an as needed basis
    public GeometryIndex getGeomIndex() {
    	
    	if(this.geomIndex == null)
    		this.geomIndex = new GeometryIndex(this);
    	
    	return this.geomIndex;
    }

    // lazy-init sample factor on an as needed basis
    public SampleFactory getSampleFactory() {
        if(this.sampleFactory == null)
            this.sampleFactory = new SampleFactory(this);

        return this.sampleFactory;	
    }

    /**
     * Calculates Transit center from median of coordinates of all transitStops if graph
     * has transit. If it doesn't it isn't calculated. (mean walue of min, max latitude and longitudes are used)
     *
     * Transit center is saved in center variable
     *
     * This speeds up calculation, but problem is that median needs to have all of latitudes/longitudes
     * in memory, this can become problematic in large installations. It works without a problem on New York State.
     * @see GraphEnvelope
     */
    public void calculateTransitCenter() {
        if (hasTransit) {

            TDoubleList latitudes = new TDoubleLinkedList();
            TDoubleList longitudes = new TDoubleLinkedList();
            Median median = new Median();

            getVertices().stream()
                .filter(v -> v instanceof TransitStop)
                .forEach(v -> {
                    latitudes.add(v.getLat());
                    longitudes.add(v.getLon());
                });

            median.setData(latitudes.toArray());
            double medianLatitude = median.evaluate();
            median = new Median();
            median.setData(longitudes.toArray());
            double medianLongitude = median.evaluate();

            this.center = new Coordinate(medianLongitude, medianLatitude);
        }
    }

    public Optional<Coordinate> getCenter() {
        return Optional.ofNullable(center);
    }

    public long getTransitServiceStarts() {
        return transitServiceStarts;
    }

    public long getTransitServiceEnds() {
        return transitServiceEnds;
    }
}
