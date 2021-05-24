package org.opentripplanner.routing.impl;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.SpatialIndex;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * <p>
 * Creates a TemporaryStreetLocation representing a location on a street that's not at an
 * intersection, based on input latitude and longitude. Instantiating this class is expensive,
 * because it creates a spatial index of all of the intersections in the graph.
 */
public class StreetVertexIndex {

  private final Graph graph;

  private final VertexLinker vertexLinker;

  /**
   * Contains only instances of {@link StreetEdge}
   */
  private final SpatialIndex edgeTree;
  private final SpatialIndex transitStopTree;
  private final SpatialIndex verticesTree;

  private static final Logger LOG = LoggerFactory.getLogger(StreetVertexIndex.class);

  /**
   * Should only be called by the graph.
   */
  public StreetVertexIndex(Graph graph) {
    this.graph = graph;
    edgeTree = new HashGridSpatialIndex<>();
    transitStopTree = new HashGridSpatialIndex<>();
    verticesTree = new HashGridSpatialIndex<>();
    vertexLinker = new VertexLinker(this.graph);
    postSetup();
  }

  private static void createHalfLocationForTest(
      TemporaryStreetLocation base, I18NString name, Coordinate nearestPoint, StreetEdge street,
      boolean endVertex, DisposableEdgeCollection tempEdges
  ) {
    StreetVertex tov = (StreetVertex) street.getToVertex();
    StreetVertex fromv = (StreetVertex) street.getFromVertex();
    LineString geometry = street.getGeometry();

    P2<LineString> geometries = getGeometry(street, nearestPoint);

    double totalGeomLength = geometry.getLength();
    double lengthRatioIn = geometries.first.getLength() / totalGeomLength;

    double lengthIn = street.getDistanceMeters() * lengthRatioIn;
    double lengthOut = street.getDistanceMeters() * (1 - lengthRatioIn);

    if (endVertex) {
      TemporaryPartialStreetEdge temporaryPartialStreetEdge = new TemporaryPartialStreetEdge(
          street,
          fromv,
          base,
          geometries.first,
          name,
          lengthIn
      );

      temporaryPartialStreetEdge.setMotorVehicleNoThruTraffic(street.isMotorVehicleNoThruTraffic());
      temporaryPartialStreetEdge.setBicycleNoThruTraffic(street.isBicycleNoThruTraffic());
      temporaryPartialStreetEdge.setStreetClass(street.getStreetClass());
      tempEdges.addEdge(temporaryPartialStreetEdge);
    }
    else {
      TemporaryPartialStreetEdge temporaryPartialStreetEdge = new TemporaryPartialStreetEdge(
          street,
          base,
          tov,
          geometries.second,
          name,
          lengthOut
      );

      temporaryPartialStreetEdge.setStreetClass(street.getStreetClass());
      temporaryPartialStreetEdge.setMotorVehicleNoThruTraffic(street.isMotorVehicleNoThruTraffic());
      temporaryPartialStreetEdge.setBicycleNoThruTraffic(street.isBicycleNoThruTraffic());
      tempEdges.addEdge(temporaryPartialStreetEdge);
    }
  }

  private static P2<LineString> getGeometry(StreetEdge e, Coordinate nearestPoint) {
    LineString geometry = e.getGeometry();
    return GeometryUtils.splitGeometryAtPoint(geometry, nearestPoint);
  }

  public VertexLinker getVertexLinker() {
    return vertexLinker;
  }

  /**
   * Get all transit stops within a given distance of a coordinate
   *
   * @return The transit stops within a certain radius of the given location.
   */
  public List<TransitStopVertex> getNearbyTransitStops(Coordinate coordinate, double radius) {
    Envelope env = new Envelope(coordinate);
    env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(radius, coordinate.y),
        SphericalDistanceLibrary.metersToDegrees(radius)
    );
    List<TransitStopVertex> nearby = getTransitStopForEnvelope(env);
    List<TransitStopVertex> results = new ArrayList<>();
    for (TransitStopVertex v : nearby) {
      if (SphericalDistanceLibrary.distance(v.getCoordinate(), coordinate) <= radius) {
        results.add(v);
      }
    }
    return results;
  }

  /**
   * Returns the vertices intersecting with the specified envelope.
   */
  @SuppressWarnings("unchecked")
  public List<Vertex> getVerticesForEnvelope(Envelope envelope) {
    List<Vertex> vertices = verticesTree.query(envelope);
    // Here we assume vertices list modifiable
    vertices.removeIf(v -> !envelope.contains(new Coordinate(v.getLon(), v.getLat())));
    return vertices;
  }

  /**
   * Return the edges whose geometry intersect with the specified envelope. Warning: edges w/o
   * geometry will not be indexed.
   */
  @SuppressWarnings("unchecked")
  public Collection<Edge> getEdgesForEnvelope(Envelope envelope) {
    List<Edge> edges = edgeTree.query(envelope);
    for (Iterator<Edge> ie = edges.iterator(); ie.hasNext(); ) {
      Edge e = ie.next();
      Envelope eenv = edgeGeometryOrStraightLine(e).getEnvelopeInternal();
      //Envelope eenv = e.getEnvelope();
        if (!envelope.intersects(eenv)) { ie.remove(); }
    }
    return edges;
  }

  /**
   * @return The transit stops within an envelope.
   */
  @SuppressWarnings("unchecked")
  public List<TransitStopVertex> getTransitStopForEnvelope(Envelope envelope) {
    List<TransitStopVertex> stopVertices = transitStopTree.query(envelope);
    stopVertices.removeIf(ts -> !envelope.intersects(new Coordinate(ts.getLon(), ts.getLat())));
    return stopVertices;
  }

  /**
   * Gets a set of vertices corresponding to the location provided. It first tries to match a
   * Stop/StopCollection by id, and if not successful it uses the coordinates if provided.
   *
   * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
   */
  public Set<Vertex> getVerticesForLocation(
      GenericLocation location, RoutingRequest options, boolean endVertex, Set<DisposableEdgeCollection> tempEdges
  ) {
    // Check if Stop/StopCollection is found by FeedScopeId
    if (location.stopId != null) {
      Set<Vertex> transitStopVertices = graph.getStopVerticesById(location.stopId);
      if (transitStopVertices != null) {
        return transitStopVertices;
      }
    }

    // Check if coordinate is provided and connect it to graph
    Coordinate coordinate = location.getCoordinate();
    if (coordinate != null) {
      //return getClosestVertex(loc, options, endVertex);
      return Collections.singleton(createVertexFromLocation(location, options, endVertex, tempEdges));
    }

    return null;
  }

  private Vertex createVertexFromLocation(
      GenericLocation location, RoutingRequest options, boolean endVertex, Set<DisposableEdgeCollection> tempEdges
  ) {
    if (endVertex) {
      LOG.debug("Finding end vertex for {}", location);
    }
    else {
      LOG.debug("Finding start vertex for {}", location);
    }

    String name;
    if (location.label == null || location.label.isEmpty()) {
      if (endVertex) {
        name = "Destination";
      }
      else {
        name = "Origin";
      }
    }
    else {
      name = location.label;
    }

    TemporaryStreetLocation temporaryStreetLocation = new TemporaryStreetLocation(
        UUID.randomUUID().toString(),
        location.getCoordinate(),
        new NonLocalizedString(name),
        endVertex
    );

    TraverseMode nonTransitMode = getTraverseModeForLinker(options, endVertex);

    tempEdges.add(vertexLinker.linkVertexForRequest(
        temporaryStreetLocation,
        new TraverseModeSet(nonTransitMode),
        endVertex ? LinkingDirection.OUTGOING : LinkingDirection.INCOMING,
        endVertex
            ? (vertex, streetVertex) -> List.of(
                new TemporaryFreeEdge(streetVertex, (TemporaryStreetLocation)vertex)
              )
            : (vertex, streetVertex) -> List.of(
                new TemporaryFreeEdge((TemporaryStreetLocation)vertex, streetVertex)
              )
    ));

    if (temporaryStreetLocation.getIncoming().isEmpty()
        && temporaryStreetLocation.getOutgoing().isEmpty()) {
      LOG.warn("Couldn't link {}", location);
    }

    temporaryStreetLocation.setWheelchairAccessible(true);

    return temporaryStreetLocation;
  }

  private TraverseMode getTraverseModeForLinker(RoutingRequest options, boolean endVertex) {
    TraverseMode nonTransitMode = TraverseMode.WALK;
    //It can be null in tests
    if (options != null) {
      TraverseModeSet modes = options.streetSubRequestModes;
      // for park and ride we will start in car mode and walk to the end vertex
      boolean parkAndRideDepart = modes.getCar() && options.parkAndRide && !endVertex;
      boolean onlyCarAvailable = modes.getCar() && !(modes.getWalk() || modes.getBicycle());
      if (onlyCarAvailable || parkAndRideDepart) {
        nonTransitMode = TraverseMode.CAR;
      }
    }
    return nonTransitMode;
  }

  @SuppressWarnings("rawtypes")
  private void postSetup() {
    for (Vertex gv : graph.getVertices()) {
      /*
       * We add all edges with geometry, skipping transit, filtering them out after. We do not
       * index transit edges as we do not need them and some GTFS do not have shape data, so
       * long straight lines between 2 faraway stations will wreck performance on a hash grid
       * spatial index.
       *
       * If one need to store transit edges in the index, we could improve the hash grid
       * rasterizing splitting long segments.
       */
      for (Edge e : gv.getOutgoing()) {
        LineString geometry = edgeGeometryOrStraightLine(e);
        Envelope env = geometry.getEnvelopeInternal();
          if (edgeTree instanceof HashGridSpatialIndex) {
              ((HashGridSpatialIndex) edgeTree).insert(geometry, e);
          }
          else { edgeTree.insert(env, e); }
      }
      if (gv instanceof TransitStopVertex) {
        Envelope env = new Envelope(gv.getCoordinate());
        transitStopTree.insert(env, gv);
      }
      Envelope env = new Envelope(gv.getCoordinate());
      verticesTree.insert(env, gv);
    }
  }

  @Override
  public String toString() {
    return getClass().getName() + " -- edgeTree: " + edgeTree.toString() + " -- verticesTree: "
        + verticesTree.toString();
  }

  /**
   * Finds the appropriate vertex for this location.
   *
   * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
   */
  public Vertex getVertexForLocationForTest(
      GenericLocation location, RoutingRequest options, boolean endVertex, Set<DisposableEdgeCollection> tempEdges
  ) {
    // Check if coordinate is provided and connect it to graph
    Coordinate coordinate = location.getCoordinate();
    if (coordinate != null) {
      //return getClosestVertex(loc, options, endVertex);
      return createVertexFromLocation(location, options, endVertex, tempEdges);
    }

    return null;
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
      String label, I18NString name, Iterable<StreetEdge> edges, Coordinate nearestPoint,
      boolean endVertex, DisposableEdgeCollection tempEdges
  ) {
    boolean wheelchairAccessible = false;

    TemporaryStreetLocation location = new TemporaryStreetLocation(
        label,
        nearestPoint,
        name,
        endVertex
    );

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
          tempEdges.addEdge(new TemporaryFreeEdge(edgeLocation, location));
        }
        else {
          tempEdges.addEdge(new TemporaryFreeEdge(location, edgeLocation));
        }
      }
      else if (SphericalDistanceLibrary.distance(nearestPoint, tov.getCoordinate()) < 1) {
        // no need to link to area edges caught on-end
        edgeLocation = tov;

        if (endVertex) {
          tempEdges.addEdge(new TemporaryFreeEdge(edgeLocation, location));
        }
        else {
          tempEdges.addEdge(new TemporaryFreeEdge(location, edgeLocation));
        }
      }
      else {
        // creates links from street head -> location -> street tail.
        createHalfLocationForTest(location, name, nearestPoint, street, endVertex, tempEdges);
      }
    }
    location.setWheelchairAccessible(wheelchairAccessible);
    return location;
  }

  private static LineString edgeGeometryOrStraightLine(Edge e) {
    LineString geometry = e.getGeometry();
    if (geometry == null) {
      Coordinate[] coordinates = new Coordinate[]{e.getFromVertex().getCoordinate(), e.getToVertex().getCoordinate()};
      geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }
    return geometry;
  }
}
