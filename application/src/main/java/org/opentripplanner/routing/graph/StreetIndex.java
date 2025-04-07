package org.opentripplanner.routing.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
 * Creates a TemporaryStreetLocation representing a location on a street that's not at an
 * intersection, based on input latitude and longitude. Instantiating this class is expensive,
 * because it creates a spatial index of all the intersections in the graph.
 */
class StreetIndex {

  private static final Logger LOG = LoggerFactory.getLogger(StreetIndex.class);

  private final Map<FeedScopedId, TransitStopVertex> stopVerticesById;
  private final ImmutableSetMultimap<FeedScopedId, TransitStopVertex> stopVerticesByParentId;

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
    var stopVertices = graph.getVerticesOfType(TransitStopVertex.class);
    this.stopVerticesById = indexStopIds(stopVertices);
    this.stopVerticesByParentId = indexStationIds(stopVertices);

    this.stationCentroidVertices = indexStationCentroids(graph);
    postSetup(graph.getVertices());
  }

  @Nullable
  TransitStopVertex findTransitStopVertex(FeedScopedId stopId) {
    return stopVerticesById.get(stopId);
  }

  /**
   * Returns the vertices intersecting with the specified envelope.
   */
  List<Vertex> getVerticesForEnvelope(Envelope envelope) {
    List<Vertex> vertices = vertexIndex.query(envelope);
    // Here we assume vertices list modifiable
    vertices.removeIf(v -> !envelope.contains(new Coordinate(v.getLon(), v.getLat())));
    return vertices;
  }

  /**
   * Return the edges whose geometry intersect with the specified envelope. Warning: edges disconnected from the graph
   * will not be indexed.
   */
  Collection<Edge> getEdgesForEnvelope(Envelope envelope) {
    return edgeIndex
      .query(envelope, Scope.PERMANENT)
      .filter(
        e ->
          e.isReachableFromGraph() &&
          envelope.intersects(edgeGeometryOrStraightLine(e).getEnvelopeInternal())
      )
      .toList();
  }

  @Override
  public String toString() {
    return (
      getClass().getName() +
      " -- edgeTree: " +
      edgeIndex.toString() +
      " -- verticesTree: " +
      vertexIndex.toString()
    );
  }

  /**
   * @param id Id of Stop, Station, MultiModalStation or GroupOfStations
   * @return The associated TransitStopVertex or all underlying TransitStopVertices
   */
  Set<TransitStopVertex> getStopOrChildStopsVertices(FeedScopedId id) {
    if (stopVerticesById.containsKey(id)) {
      return Set.of(stopVerticesById.get(id));
    } else if (stopVerticesByParentId.containsKey(id)) {
      return stopVerticesByParentId.get(id);
    } else {
      return Set.of();
    }
  }

  /**
   * @see Graph#findStopVertices(FeedScopedId)
   */
  Set<Vertex> findStopVertices(FeedScopedId id) {
    var stationVertex = stationCentroidVertices.get(id);
    if (stationVertex != null) {
      return Set.of(stationVertex);
    }
    return Collections.unmodifiableSet(getStopOrChildStopsVertices(id));
  }

  Collection<Edge> getEdgesForEnvelope(Envelope env, Scope scope) {
    return edgeIndex.query(env, scope).toList();
  }

  void insert(Edge edge, Scope scope) {
    edgeIndex.insert(edge, scope);
  }

  void remove(Edge e, Scope scope) {
    edgeIndex.remove(e, scope);
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

  private static Map<FeedScopedId, TransitStopVertex> indexStopIds(
    Collection<TransitStopVertex> vertices
  ) {
    var map = new HashMap<FeedScopedId, TransitStopVertex>();
    for (TransitStopVertex it : vertices) {
      map.put(it.getStop().getId(), it);
    }
    return Map.copyOf(map);
  }

  private static ImmutableSetMultimap<FeedScopedId, TransitStopVertex> indexStationIds(
    Collection<TransitStopVertex> vertices
  ) {
    Multimap<FeedScopedId, TransitStopVertex> map = ArrayListMultimap.create();
    vertices
      .stream()
      .filter(v -> v.getStop().isPartOfStation())
      .forEach(v -> {
        map.put(v.getStop().getParentStation().getId(), v);
      });
    return ImmutableSetMultimap.copyOf(map);
  }

  private static Map<FeedScopedId, StationCentroidVertex> indexStationCentroids(Graph graph) {
    return graph
      .getVerticesOfType(StationCentroidVertex.class)
      .stream()
      .filter(vertex -> vertex.getStation().shouldRouteToCentroid())
      .collect(Collectors.toUnmodifiableMap(v -> v.getStation().getId(), v -> v));
  }
}
