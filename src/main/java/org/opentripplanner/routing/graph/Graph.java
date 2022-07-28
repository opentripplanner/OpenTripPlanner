package org.opentripplanner.routing.graph;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.GraphUtils;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.util.ElevationUtils;
import org.opentripplanner.util.WorldEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A graph is really just one or more indexes into a set of vertexes. It used to keep edgelists for
 * each vertex, but those are in the vertex now.
 */
public class Graph implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

  public static final DrivingDirection DEFAULT_DRIVING_DIRECTION =
    DrivingDirection.RIGHT_HAND_TRAFFIC;

  public static final IntersectionTraversalCostModel DEFAULT_INTERSECTION_TRAVERSAL_COST_MODEL = new SimpleIntersectionTraversalCostModel(
    DEFAULT_DRIVING_DIRECTION
  );

  public final StreetNotesService streetNotesService = new StreetNotesService();

  private final Map<Class<?>, Serializable> services = new HashMap<>();

  /* Ideally we could just get rid of vertex labels, but they're used in tests and graph building. */
  private final Map<String, Vertex> vertices = new ConcurrentHashMap<>();

  public final transient Deduplicator deduplicator;

  public final Instant buildTime = Instant.now();
  private StopModel stopModel;

  private OpeningHoursCalendarService openingHoursCalendarService;
  private transient StreetVertexIndex streetIndex;

  //Envelope of all OSM and transit vertices. Calculated during build time
  private WorldEnvelope envelope = null;
  //ConvexHull of all the graph vertices. Generated at Graph build time.
  private Geometry convexHull = null;

  /* The preferences that were used for graph building. */
  public Preferences preferences = null;
  // TODO OTP2: This is only enabled with static bike rental
  public boolean hasBikeSharing = false;
  public boolean hasParkRide = false;
  public boolean hasBikeRide = false;

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

  private transient RealtimeVehiclePositionService vehiclePositionService;

  private DrivingDirection drivingDirection = DEFAULT_DRIVING_DIRECTION;

  private IntersectionTraversalCostModel intersectionTraversalCostModel =
    DEFAULT_INTERSECTION_TRAVERSAL_COST_MODEL;

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

  public Graph(StopModel stopModel, Deduplicator deduplicator) {
    this.stopModel = stopModel;
    this.deduplicator = deduplicator;
  }

  // Constructor for deserialization.
  public Graph() {
    this.deduplicator = new Deduplicator();
  }

  /**
   * Add the given vertex to the graph. Ideally, only vertices should add themselves to the graph,
   * when they are constructed or deserialized.
   * <p>
   * TODO OTP2 - This strategy is error prune, problematic when testing and causes a cyclic
   *           - dependency Graph -> Vertex -> Graph. A better approach is to lett the bigger
   *           - whole (Graph) create and attach its smaller parts (Vertex). A way is to create
   *           - a VertexCollection class, let the graph hold an instance of this collection,
   *           - and create factory methods for each type of Vertex in the VertexCollection.
   */
  public void addVertex(Vertex v) {
    Vertex old = vertices.put(v.getLabel(), v);
    if (old != null) {
      if (old == v) LOG.error("repeatedly added the same vertex: {}", v); else LOG.error(
        "duplicate vertex label in graph (added vertex to graph anyway): {}",
        v
      );
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

      if (e instanceof StreetEdge) {
        ((StreetEdge) e).removeAllTurnRestrictions();
      }

      if (e.fromv != null) {
        e.fromv
          .getIncoming()
          .stream()
          .filter(StreetEdge.class::isInstance)
          .map(StreetEdge.class::cast)
          .forEach(otherEdge -> {
            for (TurnRestriction turnRestriction : otherEdge.getTurnRestrictions()) {
              if (turnRestriction.to == e) {
                otherEdge.removeTurnRestriction(turnRestriction);
              }
            }
          });

        e.fromv.removeOutgoing(e);
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

  @SuppressWarnings("unchecked")
  public <T extends Serializable> T putService(Class<T> serviceType, T service) {
    return (T) services.put(serviceType, service);
  }

  public boolean hasService(Class<? extends Serializable> serviceType) {
    return services.containsKey(serviceType);
  }

  @SuppressWarnings("unchecked")
  public <T extends Serializable> T getService(Class<T> serviceType) {
    return (T) services.get(serviceType);
  }

  public <T extends Serializable> T getService(Class<T> serviceType, boolean autoCreate) {
    T t = (T) services.get(serviceType);
    if (t == null && autoCreate) {
      try {
        t = serviceType.getDeclaredConstructor().newInstance();
      } catch (
        IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | InstantiationException e
      ) {
        throw new RuntimeException(e);
      }
      services.put(serviceType, t);
    }
    return t;
  }

  public void remove(Vertex vertex) {
    vertices.remove(vertex.getLabel());
  }

  public void removeIfUnconnected(Vertex v) {
    if (v.getDegreeIn() == 0 && v.getDegreeOut() == 0) {
      remove(v);
    }
  }

  public Envelope getExtent() {
    Envelope env = new Envelope();
    for (Vertex v : getVertices()) {
      env.expandToInclude(v.getCoordinate());
    }
    return env;
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
   */
  public void index() {
    LOG.info("Index street model...");
    streetIndex = new StreetVertexIndex(this, stopModel);
    LOG.info("Index street model complete.");
  }

  public OpeningHoursCalendarService getOpeningHoursCalendarService() {
    return this.openingHoursCalendarService;
  }

  public void initOpeningHoursCalendarService(ServiceDateInterval serviceDateInterval) {
    this.openingHoursCalendarService =
      new OpeningHoursCalendarService(
        deduplicator,
        serviceDateInterval.getStart(),
        serviceDateInterval.getEnd()
      );
  }

  public StreetVertexIndex getStreetIndex() {
    //TODO refactoring transit model - thread safety
    if (this.streetIndex == null) {
      index();
    }
    return this.streetIndex;
  }

  public VertexLinker getLinker() {
    return getStreetIndex().getVertexLinker();
  }

  public int removeEdgelessVertices() {
    int removed = 0;
    List<Vertex> toRemove = new LinkedList<>();
    for (Vertex v : this.getVertices()) if (v.getDegreeOut() + v.getDegreeIn() == 0) toRemove.add(
      v
    );
    // avoid concurrent vertex map modification
    for (Vertex v : toRemove) {
      this.remove(v);
      removed += 1;
      LOG.trace("removed edgeless vertex {}", v);
    }
    return removed;
  }

  /**
   * Calculates envelope out of all OSM coordinates
   * <p>
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
   * <p>
   * If envelope is empty it creates it (This can happen with a graph without OSM data) Used when
   * adding stops to OSM envelope
   *
   * @param x the value to lower the minimum x to or to raise the maximum x to
   * @param y the value to lower the minimum y to or to raise the maximum y to
   */
  public void expandToInclude(double x, double y) {
    //Envelope can be empty if graph building is run without OSM data
    if (this.envelope == null) {
      calculateEnvelope();
    }
    this.envelope.expandToInclude(x, y);
  }

  public void initEllipsoidToGeoidDifference() {
    try {
      WorldEnvelope env = getEnvelope();
      double lat = (env.getLowerLeftLatitude() + env.getUpperRightLatitude()) / 2;
      double lon = (env.getLowerLeftLongitude() + env.getUpperRightLongitude()) / 2;
      this.ellipsoidToGeoidDifference = ElevationUtils.computeEllipsoidToGeoidDifference(lat, lon);
      LOG.info(
        "Computed ellipsoid/geoid offset at (" +
        lat +
        ", " +
        lon +
        ") as " +
        this.ellipsoidToGeoidDifference
      );
    } catch (Exception e) {
      LOG.error("Error computing ellipsoid/geoid difference");
    }
  }

  public WorldEnvelope getEnvelope() {
    return this.envelope;
  }

  public double getDistanceBetweenElevationSamples() {
    return distanceBetweenElevationSamples;
  }

  public void setDistanceBetweenElevationSamples(double distanceBetweenElevationSamples) {
    this.distanceBetweenElevationSamples = distanceBetweenElevationSamples;
    CompactElevationProfile.setDistanceBetweenSamplesM(distanceBetweenElevationSamples);
  }

  public RealtimeVehiclePositionService getVehiclePositionService() {
    if (vehiclePositionService == null) {
      vehiclePositionService = new RealtimeVehiclePositionService();
    }
    return vehiclePositionService;
  }

  public VehicleRentalStationService getVehicleRentalStationService() {
    return getService(VehicleRentalStationService.class);
  }

  public VehicleParkingService getVehicleParkingService() {
    return getService(VehicleParkingService.class);
  }

  public DrivingDirection getDrivingDirection() {
    return drivingDirection;
  }

  public void setDrivingDirection(DrivingDirection drivingDirection) {
    this.drivingDirection = drivingDirection;
  }

  public IntersectionTraversalCostModel getIntersectionTraversalModel() {
    return intersectionTraversalCostModel;
  }

  public void setIntersectionTraversalCostModel(
    IntersectionTraversalCostModel intersectionTraversalCostModel
  ) {
    this.intersectionTraversalCostModel = intersectionTraversalCostModel;
  }

  public StopModel getStopModel() {
    return stopModel;
  }

  private void readObject(ObjectInputStream inputStream)
    throws ClassNotFoundException, IOException {
    inputStream.defaultReadObject();
  }
}
