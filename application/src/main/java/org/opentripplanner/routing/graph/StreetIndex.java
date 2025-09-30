package org.opentripplanner.routing.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.routing.linking.Scope;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * <p>
 * Instantiating this class is expensive, because it creates a spatial index of all the intersections
 * in the graph.
 */
class StreetIndex {

  private static final Logger LOG = LoggerFactory.getLogger(StreetIndex.class);

  private final Map<FeedScopedId, TransitStopVertex> stopVertices;

  /**
   * This list contains transitStationVertices for the stations that are configured to route to centroid
   */
  private final Map<FeedScopedId, StationCentroidVertex> stationCentroidVertices;

  private final EdgeSpatialIndex edgeIndex;
  private final HashGridSpatialIndex<Vertex> vertexIndex;

  /**
   * Should only be called by the graph.
   */
  StreetIndex(Graph graph) {
    this.edgeIndex = new EdgeSpatialIndex();
    this.vertexIndex = new HashGridSpatialIndex<>();
    this.stopVertices = indexStopIds(graph);

    this.stationCentroidVertices = indexStationCentroids(graph);
    postSetup(graph.getVertices());
  }

  /**
   * @see Graph#findStopVertex(FeedScopedId) (FeedScopedId)
   */
  Optional<TransitStopVertex> findStopVertex(FeedScopedId id) {
    return Optional.ofNullable(stopVertices.get(id));
  }

  /**
   * @see Graph#findStationCentroidVertex(FeedScopedId)
   */
  Optional<StationCentroidVertex> findStationCentroidVertex(FeedScopedId id) {
    return Optional.ofNullable(stationCentroidVertices.get(id));
  }

  /**
   * Returns the vertices intersecting with the specified envelope.
   */
  List<Vertex> findVertices(Envelope envelope) {
    List<Vertex> vertices = vertexIndex.query(envelope);
    // Here we assume vertices list modifiable
    vertices.removeIf(v -> !envelope.contains(new Coordinate(v.getLon(), v.getLat())));
    return vertices;
  }

  /**
   * Return the edges whose geometry intersect with the specified envelope.
   * Warning: edges disconnected from the graph
   * will not be indexed.
   */
  Collection<Edge> findEdges(Envelope envelope) {
    return edgeIndex
      .query(envelope, Scope.PERMANENT)
      .filter(
        e ->
          e.isReachableFromGraph() &&
          envelope.intersects(edgeGeometryOrStraightLine(e).getEnvelopeInternal())
      )
      .toList();
  }

  Collection<Edge> findEdges(Envelope env, Scope scope) {
    return edgeIndex.query(env, scope).toList();
  }

  void insert(Edge edge, Scope scope) {
    edgeIndex.insert(edge, scope);
  }

  void remove(Edge e, Scope scope) {
    edgeIndex.remove(e, scope);
  }

  /**
   * Remove a vertex from the index.
   */
  void remove(Vertex vertex) {
    vertexIndex.remove(new Envelope(vertex.getCoordinate()), vertex);
  }

  // private methods

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

  private void postSetup(Collection<Vertex> vertices) {
    var progress = ProgressTracker.track("Index street vertex", 1000, vertices.size());
    LOG.info(progress.startMessage());

    for (Vertex gv : vertices) {
      for (Edge e : gv.getOutgoing()) {
        LineString geometry = edgeGeometryOrStraightLine(e);
        edgeIndex.insert(geometry, e, Scope.PERMANENT);
      }
      Envelope env = new Envelope(gv.getCoordinate());
      vertexIndex.insert(env, gv);

      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    }

    // Trim the sizes of the indices
    edgeIndex.compact();
    vertexIndex.compact();
    LOG.info(progress.completeMessage());
  }

  private static Map<FeedScopedId, TransitStopVertex> indexStopIds(Graph graph) {
    var vertices = graph.getVerticesOfType(TransitStopVertex.class);
    var map = new HashMap<FeedScopedId, TransitStopVertex>();
    for (TransitStopVertex it : vertices) {
      map.put(it.getStop().getId(), it);
    }
    return Map.copyOf(map);
  }

  private static Map<FeedScopedId, StationCentroidVertex> indexStationCentroids(Graph graph) {
    return graph
      .getVerticesOfType(StationCentroidVertex.class)
      .stream()
      .filter(vertex -> vertex.getStation().shouldRouteToCentroid())
      .collect(Collectors.toUnmodifiableMap(v -> v.getStation().getId(), v -> v));
  }
}
