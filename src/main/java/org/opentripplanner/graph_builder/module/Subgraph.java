package org.opentripplanner.graph_builder.module;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class Subgraph {

  private final Set<Vertex> streetVertexSet;
  private final Set<Vertex> stopsVertexSet;

  Subgraph() {
    streetVertexSet = new HashSet<>();
    stopsVertexSet = new HashSet<>();
  }

  void addVertex(Vertex vertex) {
    if (vertex instanceof TransitStopVertex) {
      stopsVertexSet.add(vertex);
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
    //TODO this is not very smart but good enough at the moment
    return streetVertexSet.iterator().next();
  }

  Iterator<Vertex> streetIterator() {
    return streetVertexSet.iterator();
  }

  Iterator<Vertex> stopIterator() {
    return stopsVertexSet.iterator();
  }

  private double minDist(Vertex v, double searchRadius) {
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
  // Distance is estimated using minimal vertex to verter search instead of computing
  // distances between graph edges. This is good enough for our heuristics.
  double distanceFromOtherGraph(StreetIndex index, double searchRadius) {
    Vertex v = getRepresentativeVertex();
    double xscale = Math.cos(v.getCoordinate().y * Math.PI / 180);
    double searchRadiusDegrees = SphericalDistanceLibrary.metersToDegrees(searchRadius);

    Envelope envelope = new Envelope();

    for (Iterator<Vertex> vIter = streetIterator(); vIter.hasNext();) {
      Vertex vx = vIter.next();
      envelope.expandToInclude(vx.getCoordinate());
    }
    for (Iterator<Vertex> vIter = stopIterator(); vIter.hasNext();) {
      Vertex vx = vIter.next();
      envelope.expandToInclude(vx.getCoordinate());
    }
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);

    return index
      .getVerticesForEnvelope(envelope)
      .stream()
      .filter(vx -> contains(vx) == false)
      .map(vx -> minDist(vx, searchRadius))
      .min(Double::compareTo)
      .orElse(searchRadius);
  }
}
