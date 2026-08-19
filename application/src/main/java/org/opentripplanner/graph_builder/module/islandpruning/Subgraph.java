package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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

class Subgraph {

  private final Set<Vertex> streetVertices;
  private final Set<TransitStopVertex> stopVertices;

  Subgraph() {
    streetVertices = new HashSet<>();
    stopVertices = new HashSet<>();
  }

  void addVertex(Vertex vertex) {
    if (vertex instanceof TransitStopVertex transitStopVertex) {
      stopVertices.add(transitStopVertex);
    } else {
      streetVertices.add(vertex);
    }
  }

  boolean contains(Vertex vertex) {
    return (streetVertices.contains(vertex) || stopVertices.contains(vertex));
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
    return streetVertices::iterator;
  }

  Iterable<TransitStopVertex> stopVertices() {
    return stopVertices::iterator;
  }

  // find minimal distance from a given vertex to vertices of this subgraph
  double vertexDistanceFromSubgraph(Vertex v, double searchRadius) {
    double d1 = computeDistance(v, searchRadius, streetVertices);
    double d2 = computeDistance(v, searchRadius, stopVertices);
    return Math.min(d1, d2);
  }

  private double computeDistance(Vertex v, double searchRadius, Set<? extends Vertex> vertices) {
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

    return graph
      .findVertices(envelope)
      .parallelStream()
      .filter(vx -> !contains(vx))
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
}
