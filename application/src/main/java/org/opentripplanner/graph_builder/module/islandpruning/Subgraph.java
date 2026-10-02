package org.opentripplanner.graph_builder.module.islandpruning;

import gnu.trove.list.array.TIntArrayList;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A set of connected vertices. Vertices are kept as {@link VertexIndex} ids, so that building
 * a subgraph does not need to touch the vertex objects themselves - at country scale that is
 * dominated by cache misses.
 */
class Subgraph {

  private final VertexIndex vertexIndex;
  private final TIntArrayList streetIds = new TIntArrayList();
  private final TIntArrayList stopIds = new TIntArrayList();
  private final List<Vertex> streetVertices;
  private final List<TransitStopVertex> stopVertices;

  /**
   * Lookup set for {@link #contains}, only built on demand: subgraphs are often huge (the main
   * component of a country graph has millions of vertices) but membership is only queried for
   * small ones.
   */
  @Nullable
  private Set<Vertex> allVertices;

  Subgraph(VertexIndex vertexIndex) {
    this.vertexIndex = vertexIndex;
    this.streetVertices = new VertexList<>(vertexIndex, streetIds);
    this.stopVertices = new VertexList<>(vertexIndex, stopIds);
  }

  /**
   * Add a vertex to the subgraph. The caller is responsible for not adding the same vertex twice.
   */
  void addVertex(Vertex vertex) {
    addVertex(vertexIndex.idOf(vertex));
  }

  /**
   * Add a vertex by its {@link VertexIndex} id. The caller is responsible for not adding the same
   * vertex twice.
   */
  void addVertex(int vertexId) {
    if (vertexIndex.isStopVertex(vertexId)) {
      stopIds.add(vertexId);
    } else {
      streetIds.add(vertexId);
    }
    allVertices = null;
  }

  boolean contains(Vertex vertex) {
    return allVertices().contains(vertex);
  }

  private Set<Vertex> allVertices() {
    if (allVertices == null) {
      allVertices = new HashSet<>(streetVertices);
      allVertices.addAll(stopVertices);
    }
    return allVertices;
  }

  int streetSize() {
    return streetVertices.size();
  }

  int stopSize() {
    return stopVertices.size();
  }

  Vertex getRepresentativeVertex() {
    // Return first OSM vertex if available
    for (var vertx : streetVertices) {
      if (vertx instanceof OsmVertex) {
        return vertx;
      }
    }

    // Otherwise fallback to what is available
    return streetVertices.iterator().next();
  }

  Iterable<Vertex> streetVertices() {
    return Collections.unmodifiableList(streetVertices);
  }

  Iterable<TransitStopVertex> stopVertices() {
    return Collections.unmodifiableList(stopVertices);
  }

  // find minimal distance from a given vertex to vertices of this subgraph
  double vertexDistanceFromSubgraph(Vertex v, double searchRadius) {
    double d1 = computeDistance(v, searchRadius, streetVertices);
    double d2 = computeDistance(v, searchRadius, stopVertices);
    return Math.min(d1, d2);
  }

  private double computeDistance(
    Vertex v,
    double searchRadius,
    Collection<? extends Vertex> vertices
  ) {
    double distance = Double.MAX_VALUE;
    Vertex clostestVertex = null;
    for (Vertex vertex : vertices) {
      var d = SphericalDistanceLibrary.fastDistance(
        v.getLat(),
        v.getLon(),
        vertex.getLat(),
        vertex.getLon()
      );
      if (d < distance) {
        clostestVertex = vertex;
        distance = d;
      }
    }
    if (clostestVertex == null) {
      return searchRadius;
    } else {
      return SphericalDistanceLibrary.distance(
        clostestVertex.getLat(),
        clostestVertex.getLon(),
        v.getLat(),
        v.getLon()
      );
    }
  }

  // Estimate distance of a subgraph from other parts of the graph.
  // For speed reasons, graph geometry only within given search radius is considered.
  // Distance is estimated using minimal vertex to vertex search instead of computing
  // distances between graph edges. This is good enough for our heuristics.
  double distanceFromOtherGraph(Graph graph, double searchRadius) {
    Vertex v = getRepresentativeVertex();
    double xscale = Math.cos((v.getCoordinate().y * Math.PI) / 180);
    double searchRadiusDegrees = SphericalDistanceLibrary.metersToDegrees(searchRadius);

    Envelope envelope = new Envelope();

    for (var i : streetVertices) {
      envelope.expandToInclude(i.getX(), i.getY());
    }
    for (var i : stopVertices) {
      envelope.expandToInclude(i.getX(), i.getY());
    }
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);

    // build the lookup set before going parallel
    Set<Vertex> members = allVertices();
    return graph
      .findVertices(envelope)
      .parallelStream()
      .filter(vx -> !members.contains(vx))
      .mapToDouble(vx -> vertexDistanceFromSubgraph(vx, searchRadius))
      .min()
      .orElse(searchRadius);
  }

  /**
   * Get a {@link Geometry} for all the contained vertices
   */
  Geometry getGeometry() {
    List<Point> points = new ArrayList<>();
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    Consumer<Vertex> vertexAdder = vertex ->
      points.add(geometryFactory.createPoint(vertex.getCoordinate()));
    streetVertices().forEach(vertexAdder);
    stopVertices().forEach(vertexAdder);

    return new MultiPoint(points.toArray(new Point[0]), geometryFactory);
  }

  /**
   * Checks whether the subgraph has only transit-stops for ferries
   *
   * @return true if only ferries stop at the subgraph and false if other or no modes are
   * stopping at the subgraph
   */
  boolean hasOnlyFerryStops() {
    for (TransitStopVertex v : stopVertices) {
      if (!v.isFerryStop()) {
        return false;
      }
    }
    return true;
  }

  /** A read-only view of a list of vertex ids as the vertices themselves. */
  private static class VertexList<T extends Vertex> extends AbstractList<T> {

    private final VertexIndex vertexIndex;
    private final TIntArrayList ids;

    VertexList(VertexIndex vertexIndex, TIntArrayList ids) {
      this.vertexIndex = vertexIndex;
      this.ids = ids;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
      return (T) vertexIndex.vertex(ids.get(index));
    }

    @Override
    public int size() {
      return ids.size();
    }
  }
}
