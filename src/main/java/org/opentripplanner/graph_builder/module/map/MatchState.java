package org.opentripplanner.graph_builder.module.map;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

public abstract class MatchState {

  private static final StreetSearchRequest REQUEST = StreetSearchRequest
    .of()
    .withMode(StreetMode.CAR)
    .build();

  protected static final double NEW_SEGMENT_PENALTY = 0.1;

  protected static final double NO_TRAVERSE_PENALTY = 20;

  public double currentError;

  public double accumulatedError;

  public MatchState parent;

  protected Edge edge;

  private double distanceAlongRoute = 0;

  public MatchState(MatchState parent, Edge edge, double distanceAlongRoute) {
    this.distanceAlongRoute = distanceAlongRoute;
    this.parent = parent;
    this.edge = edge;
    if (parent != null) {
      this.accumulatedError = parent.accumulatedError + parent.currentError;
      this.distanceAlongRoute += parent.distanceAlongRoute;
    }
  }

  public abstract List<MatchState> getNextStates();

  public Edge getEdge() {
    return edge;
  }

  public double getTotalError() {
    return accumulatedError + currentError;
  }

  public double getDistanceAlongRoute() {
    return distanceAlongRoute;
  }

  /* computes the distance, in meters, along a geometry */
  protected static double distanceAlongGeometry(
    Geometry geometry,
    LinearLocation startIndex,
    LinearLocation endIndex
  ) {
    if (endIndex == null) {
      endIndex = LinearLocation.getEndLocation(geometry);
    }
    double total = 0;
    LinearIterator it = new LinearIterator(geometry, startIndex);
    LinearLocation index = startIndex;
    Coordinate previousCoordinate = startIndex.getCoordinate(geometry);

    it.next();
    index = it.getLocation();
    while (index.compareTo(endIndex) < 0) {
      Coordinate thisCoordinate = index.getCoordinate(geometry);
      double distance = SphericalDistanceLibrary.fastDistance(previousCoordinate, thisCoordinate);
      total += distance;
      previousCoordinate = thisCoordinate;
      if (!it.hasNext()) {
        break;
      }
      it.next();
      index = it.getLocation();
    }
    //now, last bit of last segment
    Coordinate finalCoordinate = endIndex.getCoordinate(geometry);
    total += SphericalDistanceLibrary.distance(previousCoordinate, finalCoordinate);

    return total;
  }

  protected static double distance(Coordinate from, Coordinate to) {
    return SphericalDistanceLibrary.fastDistance(from, to);
  }

  protected boolean carsCanTraverse(Edge edge) {
    // should be done with a method on edge (canTraverse already exists on turnEdge)
    State s0 = new State(edge.getFromVertex(), REQUEST);
    State s1 = edge.traverse(s0);
    return s1 != null;
  }

  protected List<Edge> getOutgoingMatchableEdges(Vertex vertex) {
    List<Edge> edges = new ArrayList<>();
    for (Edge e : vertex.getOutgoing()) {
      if (!(e instanceof StreetEdge)) {
        continue;
      }
      if (e.getGeometry() == null) {
        continue;
      }
      edges.add(e);
    }
    return edges;
  }
}
