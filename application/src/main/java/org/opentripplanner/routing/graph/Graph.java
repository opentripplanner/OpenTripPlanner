package org.opentripplanner.routing.graph;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.framework.geometry.CompactElevationProfile;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.linking.Scope;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is one of the main data structures in OpenTripPlanner. It represents a mathematical object
 * called a graph (https://en.wikipedia.org/wiki/Graph_theory) relative to which many routing
 * algorithms are defined. A graph is made up of vertices and edges. These are also referred to as
 * nodes and arcs or links, but in OTP we always use the vertices and edges terminology.
 * <p>
 * In OTP1, the Graph contained vertices and edges representing the entire transportation network,
 * including edges representing both street segments and public transit lines connecting stops. In
 * OTP2, the Graph edges now represent only the street network. Transit routing is performed on
 * other data structures suited to the Raptor algorithm (the TimetableRepository). Some transit-related
 * vertices are still present in the Graph, specifically those representing transit stops,
 * entrances, and elevators. Their presence in the street graph creates a connection between the two
 * routable data structures (identifying where stops in the TimetableRepository are located relative to
 * roads).
 * <p>
 * Other data structures related to street routing, such as elevation data and vehicle parking
 * information, are also collected here as fields of the Graph. For historical reasons the Graph
 * sometimes serves as a catch-all, as it used to be the root of the object tree representing the
 * whole transportation network. This use of the Graph object is being phased out and discouraged.
 * <p>
 * In some sense the Graph is just some indexes into a set of vertices. The Graph used to hold lists
 * of edges for each vertex, but those lists are now attached to the vertices themselves.
 * <p>
 * TODO RT_AB: I favor renaming to StreetGraph to emphasize what it represents. TG agreed in review.
 */
public class Graph implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

  /** Attaches text notes to street edges, which do not affect routing. */
  public final StreetNotesService streetNotesService = new StreetNotesService();

  // Ideally we could just get rid of vertex labels, but they're used in tests and graph building.
  private final Map<VertexLabel, Vertex> vertices = new ConcurrentHashMap<>();

  /** Conserve memory by reusing immutable instances of Strings, integer arrays, etc. */
  public final transient Deduplicator deduplicator;

  @Nullable
  private final OpeningHoursCalendarService openingHoursCalendarService;

  private final transient VertexLinker linker;

  private transient StreetIndex streetIndex;

  /** The convex hull of all the graph vertices. Generated at the time the Graph is built. */
  private Geometry convexHull = null;

  /** True if OSM data was loaded into this Graph. */
  public boolean hasStreets = false;

  /**
   * The difference in meters between the WGS84 ellipsoid height and geoid height at the graph's
   * center
   */
  public Double ellipsoidToGeoidDifference = 0.0;

  /** True if this graph contains elevation data. */
  public boolean hasElevation = false;

  /** If this graph contains elevation data, the minimum elevation value. Otherwise null. */
  public Double minElevation = null;

  /** If this graph contains elevation data, the maximum elevation value. Otherwise null. */
  public Double maxElevation = null;

  /**
   * The horizontal distance across the ground between successive elevation samples in
   * CompactElevationProfile.
   */
  // TODO refactoring transit model: remove  and instead always serialize directly from and to the
  //  static variable in CompactElevationProfile in SerializedGraphObject
  private double distanceBetweenElevationSamples;

  /**
   * Hack. I've tried three different ways of generating unique labels. Previously we were just
   * tolerating edge label collisions. For some reason we're repeatedly generating splits on the
   * same edge objects, despite a comment that said it was guaranteed there would only ever be one
   * split per edge. This is going to fail as soon as we load a base OSM graph and build transit on
   * top of it.
   */
  public long nextSplitNumber = 0;

  /**
   * DataOverlay Sandbox module parameter bindings configured in the build-config, and needed when
   * creating the data overlay context when routing.
   */
  public DataOverlayParameterBindings dataOverlayParameterBindings;

  @Inject
  public Graph(
    Deduplicator deduplicator,
    @Nullable OpeningHoursCalendarService openingHoursCalendarService
  ) {
    this.deduplicator = deduplicator;
    this.openingHoursCalendarService = openingHoursCalendarService;
    this.linker = new VertexLinker(this);
  }

  public Graph(Deduplicator deduplicator) {
    this(deduplicator, null);
  }

  /** Constructor for deserialization. */
  public Graph() {
    this(new Deduplicator(), null);
  }

  /** Add the given vertex to the graph. */
  public void addVertex(Vertex v) {
    Vertex old = vertices.put(v.getLabel(), v);
    if (old != null) {
      if (old == v) {
        LOG.error("repeatedly added the same vertex: {}", v);
      } else {
        LOG.error(
          "duplicate vertex label in graph (added vertex to graph anyway): {}",
          v.getLabel()
        );
      }
    }
  }

  /**
   * Removes a permanent edge from the graph. This method is not thread-safe.
   *
   * @param e The edge to be removed
   */
  public void removeEdge(Edge e) {
    removeEdge(e, Scope.PERMANENT);
  }

  public void removeEdge(Edge e, Scope scope) {
    streetNotesService.removeStaticNotes(e);
    e.remove();
    if (streetIndex != null) {
      streetIndex.remove(e, scope);
    }
  }

  /**
   * Fetching a vertex by its by label. This is convenient in tests and such, but avoid using in general.
   *
   * @see VertexLabel
   */
  @VisibleForTesting
  @Nullable
  public Vertex getVertex(VertexLabel label) {
    return vertices.get(label);
  }

  /**
   * Converts the input to a string-based label and looks it up in the graph. Remember that there
   * are other, non-string vertex labels for which this method will not work.
   * @see VertexLabel
   */
  @VisibleForTesting
  @Nullable
  public Vertex getVertex(String label) {
    return vertices.get(VertexLabel.string(label));
  }

  /**
   * Get all the vertices in the graph.
   */
  public Collection<Vertex> getVertices() {
    return this.vertices.values();
  }

  public <T extends Vertex> List<T> getVerticesOfType(Class<T> cls) {
    return this.getVertices()
      .stream()
      .filter(cls::isInstance)
      .map(cls::cast)
      .collect(Collectors.toList());
  }

  /**
   * Return the vertex corresponding to the stop id, or null.
   */
  @Nullable
  public TransitStopVertex getStopVertexForStopId(FeedScopedId id) {
    return streetIndex.findTransitStopVertex(id);
  }

  /**
   * If the {@code id} is a stop id return a set with a single element.
   * If it is a station id return a set containing all child stop vertices, or an empty
   * set otherwise.
   */
  public Set<TransitStopVertex> findStopOrChildStopsVertices(FeedScopedId stopId) {
    requireIndex();
    return streetIndex.getStopOrChildStopsVertices(stopId);
  }

  /**
   * Return all the edges in the graph. Derived from vertices on demand.
   */
  public Collection<Edge> getEdges() {
    Set<Edge> edges = new HashSet<>();
    for (Vertex v : this.getVertices()) {
      edges.addAll(v.getOutgoing());
    }
    return edges;
  }

  public <T extends Edge> List<T> getEdgesOfType(Class<T> cls) {
    return this.getEdges()
      .stream()
      .filter(cls::isInstance)
      .map(cls::cast)
      .collect(Collectors.toList());
  }

  /**
   * Return only the StreetEdges in the graph.
   */
  public Collection<StreetEdge> getStreetEdges() {
    return getEdgesOfType(StreetEdge.class);
  }

  public boolean containsVertex(Vertex v) {
    return (v != null) && vertices.get(v.getLabel()) == v;
  }

  public void remove(Vertex vertex) {
    vertices.remove(vertex.getLabel());
  }

  public void removeIfUnconnected(Vertex v) {
    if (v.getDegreeIn() == 0 && v.getDegreeOut() == 0) {
      remove(v);
    }
  }

  public int countVertices() {
    return vertices.size();
  }

  /**
   * Find the total number of edges in this Graph. There are assumed to be no Edges in an incoming
   * edge list that are not in an outgoing edge list.
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
   * Perform indexing on vertices, edges and create transient data structures. This used to be done
   * in readObject methods upon deserialization, but stand-alone mode now allows passing graphs from
   * graphbuilder to server in memory, without a round trip through serialization.
   * <p>
   * TODO OTP2 - Indexing the streetIndex is not something that should be delegated outside the
   *           - graph. This allows a module to index the streetIndex BEFORE another module add
   *           - something that should go into the index; Hence, inconsistent data.
   */
  public void index() {
    LOG.info("Index street model...");
    streetIndex = new StreetIndex(this);
    LOG.info("Index street model complete.");
  }

  @Nullable
  public OpeningHoursCalendarService getOpeningHoursCalendarService() {
    return this.openingHoursCalendarService;
  }

  /**
   * Find all vertices inside the bounding box defined by {@code env}.
   */
  public Collection<Vertex> findVertices(Envelope env) {
    requireIndex();
    return streetIndex.getVerticesForEnvelope(env);
  }

  /**
   * Get the street vertices for an id. If the id corresponds to a regular stop we will return the
   * coordinate for the stop.
   * If the id corresponds to a station we will either return the coordinates of the child stops or
   * the station centroid if the station is configured to route to centroid.
   */
  public Set<Vertex> findStopVertices(FeedScopedId stopId) {
    return streetIndex.findStopVertices(stopId);
  }

  /**
   * Find all permanent edges inside the bounding box defined by {@code env}.
   */
  public Collection<Edge> findEdges(Envelope env) {
    requireIndex();
    return streetIndex.getEdgesForEnvelope(env);
  }

  /**
   * Find all edges with the given scope inside the bounding box defined by {@code env}.
   */
  public Collection<Edge> findEdges(Envelope env, Scope scope) {
    return streetIndex.getEdgesForEnvelope(env, scope);
  }

  /**
   * Insert edge into the index with the give scope.
   */
  public void insert(StreetEdge edge, Scope scope) {
    requireIndex();
    streetIndex.insert(edge, scope);
  }

  /**
   * Get VertexLinker, safe to use while routing, but do not use during graph build.
   * @see #getLinkerSafe()
   */
  public VertexLinker getLinker() {
    requireIndex();
    return linker;
  }

  /**
   * Get VertexLinker during graph build, both OSM street data and transit data must be loaded
   * before calling this.
   */
  public VertexLinker getLinkerSafe() {
    indexIfNotIndexed();
    return linker;
  }

  /**
   * Calculates convexHull of all the vertices during build time
   */
  public void calculateConvexHull() {
    convexHull = GeometryUtils.makeConvexHull(getVertices(), Vertex::getCoordinate);
  }

  /**
   * @return calculated convexHull;
   */
  public Geometry getConvexHull() {
    return convexHull;
  }

  public void initEllipsoidToGeoidDifference(double value, double lat, double lon) {
    this.ellipsoidToGeoidDifference = value;
    LOG.info(
      "Computed ellipsoid/geoid offset at ({}, {}) as {}",
      lat,
      lon,
      this.ellipsoidToGeoidDifference
    );
  }

  public double getDistanceBetweenElevationSamples() {
    return distanceBetweenElevationSamples;
  }

  public void setDistanceBetweenElevationSamples(double distanceBetweenElevationSamples) {
    this.distanceBetweenElevationSamples = distanceBetweenElevationSamples;
    CompactElevationProfile.setDistanceBetweenSamplesM(distanceBetweenElevationSamples);
  }

  private void indexIfNotIndexed() {
    if (streetIndex == null) {
      index();
    }
  }

  private void requireIndex() {
    if (streetIndex == null) {
      throw new IllegalStateException("Graph must be indexed before querying.");
    }
  }
}
