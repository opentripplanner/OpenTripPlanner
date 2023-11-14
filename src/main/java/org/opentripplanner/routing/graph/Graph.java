package org.opentripplanner.routing.graph;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.framework.geometry.CompactElevationProfile;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.StopModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A graph is really just one or more indexes into a set of vertexes. It used to keep edgelists for
 * each vertex, but those are in the vertex now.
 */
public class Graph implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

  public final StreetNotesService streetNotesService = new StreetNotesService();

  /* Ideally we could just get rid of vertex labels, but they're used in tests and graph building. */
  private final Map<VertexLabel, Vertex> vertices = new ConcurrentHashMap<>();

  public final transient Deduplicator deduplicator;

  public final Instant buildTime = Instant.now();

  @Nullable
  private final OpeningHoursCalendarService openingHoursCalendarService;

  private transient StreetIndex streetIndex;

  //ConvexHull of all the graph vertices. Generated at Graph build time.
  private Geometry convexHull = null;

  /* The preferences that were used for graph building. */
  public Preferences preferences = null;

  /** True if OSM data was loaded into this Graph. */
  public boolean hasStreets = false;

  /**
   * Have bike parks already been linked to the graph. As the linking happens twice if a base graph
   * is used, we store information on whether bike park linking should be skipped.
   */
  public boolean hasLinkedBikeParks = false;
  /**
   * The difference in meters between the WGS84 ellipsoid height and geoid height at the graph's
   * center
   */
  public Double ellipsoidToGeoidDifference = 0.0;
  /**
   * Does this graph contain elevation data?
   */
  public boolean hasElevation = false;
  /**
   * If this graph contains elevation data, the minimum value.
   */
  public Double minElevation = null;
  /**
   * If this graph contains elevation data, the maximum value.
   */
  public Double maxElevation = null;

  /** The distance between elevation samples used in CompactElevationProfile. */
  // TODO refactoring transit model: remove  and instead always serialize directly from and to the
  //  static variable in CompactElevationProfile in SerializedGraphObject
  private double distanceBetweenElevationSamples;

  private final VehicleParkingService vehicleParkingService = new VehicleParkingService();
  private FareService fareService;

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
  private LuceneIndex luceneIndex;

  @Inject
  public Graph(
    Deduplicator deduplicator,
    @Nullable OpeningHoursCalendarService openingHoursCalendarService
  ) {
    this.deduplicator = deduplicator;
    this.openingHoursCalendarService = openingHoursCalendarService;
  }

  public Graph(Deduplicator deduplicator) {
    this(deduplicator, null);
  }

  /** Constructor for deserialization. */
  public Graph() {
    this(new Deduplicator(), null);
  }

  /**
   * Add the given vertex to the graph.
   */
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
   * Removes an edge from the graph. This method is not thread-safe.
   *
   * @param e The edge to be removed
   */
  public void removeEdge(Edge e) {
    if (e != null) {
      streetNotesService.removeStaticNotes(e);

      e.remove();
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

  @Nullable
  public TransitStopVertex getStopVertexForStopId(FeedScopedId id) {
    return streetIndex.findTransitStopVertices(id);
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
  public void index(StopModel stopModel) {
    LOG.info("Index street model...");
    streetIndex = new StreetIndex(this, stopModel);
    LOG.info("Index street model complete.");
  }

  @Nullable
  public OpeningHoursCalendarService getOpeningHoursCalendarService() {
    return this.openingHoursCalendarService;
  }

  /**
   * Get streetIndex, safe to use while routing, but do not use during graph build.
   * @see #getStreetIndexSafe(StopModel)
   */
  public StreetIndex getStreetIndex() {
    return this.streetIndex;
  }

  /**
   * Get streetIndex during graph build, both OSM street data and transit data must be loaded
   * before calling this.
   */
  public StreetIndex getStreetIndexSafe(StopModel stopModel) {
    indexIfNotIndexed(stopModel);
    return this.streetIndex;
  }

  /**
   * Get VertexLinker, safe to use while routing, but do not use during graph build.
   * @see #getLinkerSafe(StopModel)
   */
  public VertexLinker getLinker() {
    return streetIndex.getVertexLinker();
  }

  /**
   * Get VertexLinker during graph build, both OSM street data and transit data must be loaded
   * before calling this.
   */
  public VertexLinker getLinkerSafe(StopModel stopModel) {
    indexIfNotIndexed(stopModel);
    return streetIndex.getVertexLinker();
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

  @Nonnull
  public VehicleParkingService getVehicleParkingService() {
    return vehicleParkingService;
  }

  public FareService getFareService() {
    return fareService;
  }

  public void setFareService(FareService fareService) {
    this.fareService = fareService;
  }

  public LuceneIndex getLuceneIndex() {
    return luceneIndex;
  }

  public void setLuceneIndex(LuceneIndex luceneIndex) {
    this.luceneIndex = luceneIndex;
  }

  private void indexIfNotIndexed(StopModel stopModel) {
    if (streetIndex == null) {
      index(stopModel);
    }
  }
}
