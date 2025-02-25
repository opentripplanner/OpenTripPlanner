package org.opentripplanner.routing.graph.index;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.SplitLineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.Scope;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * <p>
 * Creates a TemporaryStreetLocation representing a location on a street that's not at an
 * intersection, based on input latitude and longitude. Instantiating this class is expensive,
 * because it creates a spatial index of all of the intersections in the graph.
 */
public class StreetIndex {

  private static final Logger LOG = LoggerFactory.getLogger(StreetIndex.class);

  private final SiteRepository siteRepository;

  private final VertexLinker vertexLinker;

  private final Map<FeedScopedId, TransitStopVertex> transitStopVertices;

  /**
   * This list contains transitStationVertices for the stations that are configured to route to centroid
   */
  private final Map<FeedScopedId, StationCentroidVertex> stationCentroidVertices;

  private final EdgeSpatialIndex edgeSpatialIndex;
  private final HashGridSpatialIndex<Vertex> verticesTree;

  /**
   * Should only be called by the graph.
   */
  public StreetIndex(Graph graph, SiteRepository siteRepository) {
    this.siteRepository = siteRepository;
    this.edgeSpatialIndex = new EdgeSpatialIndex();
    this.verticesTree = new HashGridSpatialIndex<>();
    this.vertexLinker = new VertexLinker(graph, siteRepository, edgeSpatialIndex);
    this.transitStopVertices = toImmutableMap(graph.getVerticesOfType(TransitStopVertex.class));
    this.stationCentroidVertices = createStationCentroidVertexMap(graph);
    postSetup(graph.getVertices());
  }

  /**
   * Creates a TemporaryStreetLocation on the given street (set of PlainStreetEdges). How far along
   * is controlled by the location parameter, which represents a distance along the edge between 0
   * (the from vertex) and 1 (the to vertex).
   *
   * @param edges A collection of nearby edges, which represent one street.
   * @return the new TemporaryStreetLocation
   */
  public static TemporaryStreetLocation createTemporaryStreetLocationForTest(
    String label,
    I18NString name,
    Iterable<StreetEdge> edges,
    Coordinate nearestPoint,
    boolean endVertex,
    DisposableEdgeCollection tempEdges
  ) {
    boolean wheelchairAccessible = false;

    var location = new TemporaryStreetLocation(label, nearestPoint, name);

    for (StreetEdge street : edges) {
      Vertex fromv = street.getFromVertex();
      Vertex tov = street.getToVertex();
      wheelchairAccessible |= street.isWheelchairAccessible();

      /* forward edges and vertices */
      Vertex edgeLocation;
      if (SphericalDistanceLibrary.distance(nearestPoint, fromv.getCoordinate()) < 1) {
        // no need to link to area edges caught on-end
        edgeLocation = fromv;

        if (endVertex) {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(edgeLocation, location));
        } else {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(location, edgeLocation));
        }
      } else if (SphericalDistanceLibrary.distance(nearestPoint, tov.getCoordinate()) < 1) {
        // no need to link to area edges caught on-end
        edgeLocation = tov;

        if (endVertex) {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(edgeLocation, location));
        } else {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(location, edgeLocation));
        }
      } else {
        // creates links from street head -> location -> street tail.
        createHalfLocationForTest(location, name, nearestPoint, street, endVertex, tempEdges);
      }
    }
    location.setWheelchairAccessible(wheelchairAccessible);
    return location;
  }

  public VertexLinker getVertexLinker() {
    return vertexLinker;
  }

  @Nullable
  public TransitStopVertex findTransitStopVertices(FeedScopedId stopId) {
    return transitStopVertices.get(stopId);
  }

  /**
   * Returns the vertices intersecting with the specified envelope.
   */
  public List<Vertex> getVerticesForEnvelope(Envelope envelope) {
    List<Vertex> vertices = verticesTree.query(envelope);
    // Here we assume vertices list modifiable
    vertices.removeIf(v -> !envelope.contains(new Coordinate(v.getLon(), v.getLat())));
    return vertices;
  }

  /**
   * Return the edges whose geometry intersect with the specified envelope. Warning: edges disconnected from the graph
   * will not be indexed.
   */
  public Collection<Edge> getEdgesForEnvelope(Envelope envelope) {
    return edgeSpatialIndex
      .query(envelope, Scope.PERMANENT)
      .filter(e ->
        e.isReachableFromGraph() &&
        envelope.intersects(edgeGeometryOrStraightLine(e).getEnvelopeInternal())
      )
      .toList();
  }

  /**
   * Gets a set of vertices corresponding to the location provided. It first tries to match one of
   * the stop or station types by id, and if not successful it uses the coordinates if provided.
   *
   * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
   */
  @Nullable
  public Set<Vertex> getStreetVerticesForLocation(
    GenericLocation location,
    StreetMode streetMode,
    boolean endVertex,
    Set<DisposableEdgeCollection> tempEdges
  ) {
    // Differentiate between driving and non-driving, as driving is not available from transit stops
    TraverseMode nonTransitMode = getTraverseModeForLinker(streetMode, endVertex);

    if (nonTransitMode.isInCar()) {
      // Fetch coordinate from stop, if not given in request
      if (location.stopId != null && location.getCoordinate() == null) {
        var coordinate = siteRepository.getCoordinateById(location.stopId);
        if (coordinate != null) {
          location =
            new GenericLocation(
              location.label,
              location.stopId,
              coordinate.latitude(),
              coordinate.longitude()
            );
        }
      }
    } else {
      // Check if Stop/StopCollection is found by FeedScopeId
      if (location.stopId != null) {
        var streetVertices = getStreetVerticesById(location.stopId);
        if (!streetVertices.isEmpty()) {
          return streetVertices;
        }
      }
    }

    // Check if coordinate is provided and connect it to graph
    if (location.getCoordinate() != null) {
      return Set.of(
        createVertexFromCoordinate(
          location.getCoordinate(),
          location.label,
          streetMode,
          endVertex,
          tempEdges
        )
      );
    }

    return null;
  }

  @Override
  public String toString() {
    return (
      getClass().getName() +
      " -- edgeTree: " +
      edgeSpatialIndex.toString() +
      " -- verticesTree: " +
      verticesTree.toString()
    );
  }

  /**
   * Create the appropriate vertex for this coordinate.
   *
   * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
   */
  public Vertex createVertexForCoordinateForTest(
    Coordinate location,
    StreetMode streetMode,
    boolean endVertex,
    Set<DisposableEdgeCollection> tempEdges
  ) {
    return createVertexFromCoordinate(location, null, streetMode, endVertex, tempEdges);
  }

  /**
   * @param id Id of Stop, Station, MultiModalStation or GroupOfStations
   * @return The associated TransitStopVertex or all underlying TransitStopVertices
   */
  public Set<TransitStopVertex> getStopOrChildStopsVertices(FeedScopedId id) {
    return siteRepository
      .findStopOrChildStops(id)
      .stream()
      .filter(RegularStop.class::isInstance)
      .map(RegularStop.class::cast)
      .map(it -> transitStopVertices.get(it.getId()))
      .collect(Collectors.toSet());
  }

  /**
   * Get the street vertices for an id. If the id corresponds to a regular stop we will return the
   * coordinate for the stop.
   * If the id corresponds to a station we will either return the coordinates of the child stops or
   * the station centroid if the station is configured to route to centroid.
   */
  public Set<Vertex> getStreetVerticesById(FeedScopedId id) {
    var stationVertex = stationCentroidVertices.get(id);
    if (stationVertex != null) {
      return Set.of(stationVertex);
    }
    return Collections.unmodifiableSet(getStopOrChildStopsVertices(id));
  }

  private static void createHalfLocationForTest(
    TemporaryStreetLocation base,
    I18NString name,
    Coordinate nearestPoint,
    StreetEdge street,
    boolean endVertex,
    DisposableEdgeCollection tempEdges
  ) {
    StreetVertex tov = (StreetVertex) street.getToVertex();
    StreetVertex fromv = (StreetVertex) street.getFromVertex();
    LineString geometry = street.getGeometry();

    SplitLineString geometries = getGeometry(street, nearestPoint);

    double totalGeomLength = geometry.getLength();
    double lengthRatioIn = geometries.beginning().getLength() / totalGeomLength;

    double lengthIn = street.getDistanceMeters() * lengthRatioIn;
    double lengthOut = street.getDistanceMeters() * (1 - lengthRatioIn);

    if (endVertex) {
      TemporaryPartialStreetEdge tpse = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(street)
        .withFromVertex(fromv)
        .withToVertex(base)
        .withGeometry(geometries.beginning())
        .withName(name)
        .withMeterLength(lengthIn)
        .withMotorVehicleNoThruTraffic(street.isMotorVehicleNoThruTraffic())
        .withBicycleNoThruTraffic(street.isBicycleNoThruTraffic())
        .withWalkNoThruTraffic(street.isWalkNoThruTraffic())
        .withLink(street.isLink())
        .buildAndConnect();
      tempEdges.addEdge(tpse);
    } else {
      TemporaryPartialStreetEdge tpse = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(street)
        .withFromVertex(base)
        .withToVertex(tov)
        .withGeometry(geometries.ending())
        .withName(name)
        .withMeterLength(lengthOut)
        .withLink(street.isLink())
        .withMotorVehicleNoThruTraffic(street.isMotorVehicleNoThruTraffic())
        .withBicycleNoThruTraffic(street.isBicycleNoThruTraffic())
        .withWalkNoThruTraffic(street.isWalkNoThruTraffic())
        .buildAndConnect();
      tempEdges.addEdge(tpse);
    }
  }

  private static SplitLineString getGeometry(StreetEdge e, Coordinate nearestPoint) {
    LineString geometry = e.getGeometry();
    return GeometryUtils.splitGeometryAtPoint(geometry, nearestPoint);
  }

  private static LineString edgeGeometryOrStraightLine(Edge e) {
    LineString geometry = e.getGeometry();
    if (geometry == null) {
      Coordinate[] coordinates = new Coordinate[] {
        e.getFromVertex().getCoordinate(),
        e.getToVertex().getCoordinate(),
      };
      geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }
    return geometry;
  }

  private Vertex createVertexFromCoordinate(
    Coordinate coordinate,
    @Nullable String label,
    StreetMode streetMode,
    boolean endVertex,
    Set<DisposableEdgeCollection> tempEdges
  ) {
    if (endVertex) {
      LOG.debug("Creating end vertex for {}", coordinate);
    } else {
      LOG.debug("Creating start vertex for {}", coordinate);
    }

    I18NString name;
    if (label == null || label.isEmpty()) {
      if (endVertex) {
        name = new LocalizedString("destination");
      } else {
        name = new LocalizedString("origin");
      }
    } else {
      name = new NonLocalizedString(label);
    }

    String id = UUID.randomUUID().toString();
    var temporaryStreetLocation = new TemporaryStreetLocation(id, coordinate, name);

    TraverseMode nonTransitMode = getTraverseModeForLinker(streetMode, endVertex);

    tempEdges.add(
      vertexLinker.linkVertexForRequest(
        temporaryStreetLocation,
        new TraverseModeSet(nonTransitMode),
        endVertex ? LinkingDirection.OUTGOING : LinkingDirection.INCOMING,
        endVertex
          ? (vertex, streetVertex) ->
            List.of(
              TemporaryFreeEdge.createTemporaryFreeEdge(
                streetVertex,
                (TemporaryStreetLocation) vertex
              )
            )
          : (vertex, streetVertex) ->
            List.of(
              TemporaryFreeEdge.createTemporaryFreeEdge(
                (TemporaryStreetLocation) vertex,
                streetVertex
              )
            )
      )
    );

    if (
      temporaryStreetLocation.getIncoming().isEmpty() &&
      temporaryStreetLocation.getOutgoing().isEmpty()
    ) {
      LOG.warn("Couldn't link {}", coordinate);
    }

    temporaryStreetLocation.setWheelchairAccessible(true);

    return temporaryStreetLocation;
  }

  private TraverseMode getTraverseModeForLinker(StreetMode streetMode, boolean endVertex) {
    TraverseMode nonTransitMode = TraverseMode.WALK;
    // for park and ride we will start in car mode and walk to the end vertex
    boolean parkAndRideDepart = streetMode == StreetMode.CAR_TO_PARK && !endVertex;
    boolean onlyCarAvailable = streetMode == StreetMode.CAR;
    if (onlyCarAvailable || parkAndRideDepart) {
      nonTransitMode = TraverseMode.CAR;
    }
    return nonTransitMode;
  }

  private void postSetup(Collection<Vertex> vertices) {
    var progress = ProgressTracker.track("Index street vertex", 1000, vertices.size());
    LOG.info(progress.startMessage());

    for (Vertex gv : vertices) {
      for (Edge e : gv.getOutgoing()) {
        LineString geometry = edgeGeometryOrStraightLine(e);
        edgeSpatialIndex.insert(geometry, e, Scope.PERMANENT);
      }
      Envelope env = new Envelope(gv.getCoordinate());
      verticesTree.insert(env, gv);

      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    }

    // Trim the sizes of the indices
    edgeSpatialIndex.compact();
    verticesTree.compact();
    LOG.info(progress.completeMessage());
  }

  private static Map<FeedScopedId, TransitStopVertex> toImmutableMap(
    Collection<TransitStopVertex> vertices
  ) {
    var map = new HashMap<FeedScopedId, TransitStopVertex>();
    for (TransitStopVertex it : vertices) {
      map.put(it.getStop().getId(), it);
    }
    return Map.copyOf(map);
  }

  private static Map<FeedScopedId, StationCentroidVertex> createStationCentroidVertexMap(
    Graph graph
  ) {
    return graph
      .getVerticesOfType(StationCentroidVertex.class)
      .stream()
      .filter(vertex -> vertex.getStation().shouldRouteToCentroid())
      .collect(Collectors.toUnmodifiableMap(v -> v.getStation().getId(), v -> v));
  }
}
