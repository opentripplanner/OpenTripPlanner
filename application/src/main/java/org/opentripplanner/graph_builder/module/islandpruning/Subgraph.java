package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.basic.TransitMode;

class Subgraph {

  private final Set<Vertex> streetVertexSet;
  private final Set<TransitStopVertex> stopsVertexSet;

  Subgraph() {
    streetVertexSet = new HashSet<>();
    stopsVertexSet = new HashSet<>();
  }

  void addVertex(Vertex vertex) {
    if (vertex instanceof TransitStopVertex transitStopVertex) {
      stopsVertexSet.add(transitStopVertex);
    } else {
      streetVertexSet.add(vertex);
    }
  }

  boolean contains(Vertex vertex) {
    return (streetVertexSet.contains(vertex) || stopsVertexSet.contains(vertex));
  }

  int streetSize() {
    return streetVertexSet.size();
  }

  int stopSize() {
    return stopsVertexSet.size();
  }

  Vertex getRepresentativeVertex() {
    // Return first OSM vertex if available
    for (var vertx : streetVertexSet) {
      if (vertx instanceof OsmVertex) {
        return vertx;
      }
    }

    // Otherwise fallback to what is available
    return streetVertexSet.iterator().next();
  }

  Iterator<Vertex> streetIterator() {
    return streetVertexSet.iterator();
  }

  Iterator<TransitStopVertex> stopIterator() {
    return stopsVertexSet.iterator();
  }

  // find minimal distance from a given vertex to vertices of this subgraph
  double vertexDistanceFromSubgraph(Vertex v, double searchRadius) {
    double d1 = streetVertexSet
      .stream()
      .map(x -> SphericalDistanceLibrary.distance(x.getCoordinate(), v.getCoordinate()))
      .min(Double::compareTo)
      .orElse(searchRadius);
    double d2 = stopsVertexSet
      .stream()
      .map(x -> SphericalDistanceLibrary.distance(x.getCoordinate(), v.getCoordinate()))
      .min(Double::compareTo)
      .orElse(searchRadius);
    return Math.min(d1, d2);
  }

  // Estimate distance of a subgraph from other parts of the graph.
  // For speed reasons, graph geometry only within given search radius is considered.
  // Distance is estimated using minimal vertex to vertex search instead of computing
  // distances between graph edges. This is good enough for our heuristics.
  double distanceFromOtherGraph(StreetIndex index, double searchRadius) {
    Vertex v = getRepresentativeVertex();
    double xscale = Math.cos((v.getCoordinate().y * Math.PI) / 180);
    double searchRadiusDegrees = SphericalDistanceLibrary.metersToDegrees(searchRadius);

    Envelope envelope = new Envelope();

    for (Iterator<Vertex> vIter = streetIterator(); vIter.hasNext();) {
      Vertex vx = vIter.next();
      envelope.expandToInclude(vx.getCoordinate());
    }
    for (TransitStopVertex vx : stopsVertexSet) {
      envelope.expandToInclude(vx.getCoordinate());
    }
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);

    return index
      .getVerticesForEnvelope(envelope)
      .stream()
      .filter(vx -> !contains(vx))
      .map(vx -> vertexDistanceFromSubgraph(vx, searchRadius))
      .min(Double::compareTo)
      .orElse(searchRadius);
  }

  /**
   * Get a {@link Geometry for all the contained vertices}
   */
  Geometry getGeometry() {
    List<Point> points = new ArrayList<>();
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    Consumer<Vertex> vertexAdder = vertex ->
      points.add(geometryFactory.createPoint(vertex.getCoordinate()));
    streetIterator().forEachRemaining(vertexAdder);
    stopIterator().forEachRemaining(vertexAdder);

    return new MultiPoint(points.toArray(new Point[0]), geometryFactory);
  }

  /**
   * Checks whether the subgraph has only transit-stops for ferries
   *
   * @return true if only ferries stop at the subgraph and false if other or no modes are
   * stopping at the subgraph
   */
  boolean hasOnlyFerryStops() {
    for (TransitStopVertex v : stopsVertexSet) {
      Set<TransitMode> modes = v.getModes();
      // test if stop has other transit modes than FERRY
      if (!modes.contains(TransitMode.FERRY)) {
        return false;
      }
    }
    return true;
  }
}
