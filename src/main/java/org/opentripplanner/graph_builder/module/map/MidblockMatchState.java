package org.opentripplanner.graph_builder.module.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.locationtech.jts.util.AssertionFailedException;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;

public class MidblockMatchState extends MatchState {

  private static final double MAX_ERROR = 1000;

  private final LinearLocation edgeIndex;
  private final Geometry edgeGeometry;
  private final LocationIndexedLine indexedEdge;
  public LinearLocation routeIndex;
  Geometry routeGeometry;

  public MidblockMatchState(
    MatchState parent,
    Geometry routeGeometry,
    Edge edge,
    LinearLocation routeIndex,
    LinearLocation edgeIndex,
    double error,
    double distanceAlongRoute
  ) {
    super(parent, edge, distanceAlongRoute);
    this.routeGeometry = routeGeometry;
    this.routeIndex = routeIndex;
    this.edgeIndex = edgeIndex;

    edgeGeometry = edge.getGeometry();
    indexedEdge = new LocationIndexedLine(edgeGeometry);
    currentError = error;
  }

  @Override
  public List<MatchState> getNextStates() {
    ArrayList<MatchState> nextStates = new ArrayList<>();
    if (routeIndex.getSegmentIndex() == routeGeometry.getNumPoints() - 1) {
      // this has either hit the end, or gone off the end. It's not real clear which.
      // for now, let's assume it means that the ending is somewhere along this edge,
      // so we return an end state
      Coordinate pt = routeIndex.getCoordinate(routeGeometry);
      double error = distance(pt, edgeIndex.getCoordinate(edgeGeometry));
      nextStates.add(new EndMatchState(this, error, 0));
      return nextStates;
    }

    LinearIterator it = new LinearIterator(routeGeometry, routeIndex);
    if (it.hasNext()) {
      it.next();
      LinearLocation routeSuccessor = it.getLocation();

      // now we want to see where this new point is in terms of the edge's geometry
      Coordinate newRouteCoord = routeSuccessor.getCoordinate(routeGeometry);
      LinearLocation newEdgeIndex = indexedEdge.project(newRouteCoord);

      Coordinate edgeCoord = newEdgeIndex.getCoordinate(edgeGeometry);
      if (newEdgeIndex.compareTo(edgeIndex) <= 0) {
        // we must make forward progress along the edge... or go to the next edge
        /* this should not require the try/catch, but there is a bug in JTS */
        try {
          LinearLocation projected2 = indexedEdge.indexOfAfter(edgeCoord, edgeIndex);
          //another bug in JTS
          if (Double.isNaN(projected2.getSegmentFraction())) {
            // we are probably moving backwards
            return Collections.emptyList();
          } else {
            newEdgeIndex = projected2;
            if (newEdgeIndex.equals(edgeIndex)) {
              return Collections.emptyList();
            }
          }
          edgeCoord = newEdgeIndex.getCoordinate(edgeGeometry);
        } catch (AssertionFailedException e) {
          // we are not making progress, so just return an empty list
          return Collections.emptyList();
        }
      }

      if (newEdgeIndex.getSegmentIndex() == edgeGeometry.getNumPoints() - 1) {
        // we might choose to continue from the end of the edge and a point mid-way
        // along this route segment

        // find nearest point that makes progress along the route
        Vertex toVertex = edge.getToVertex();
        Coordinate endCoord = toVertex.getCoordinate();
        LocationIndexedLine indexedRoute = new LocationIndexedLine(routeGeometry);

        // FIXME: it would be better to do this project/indexOfAfter in one step
        // as the two-step version could snap to a bad place and be unable to escape.

        LinearLocation routeProjectedEndIndex = indexedRoute.project(endCoord);
        Coordinate routeProjectedEndCoord = routeProjectedEndIndex.getCoordinate(routeGeometry);

        if (routeProjectedEndIndex.compareTo(routeIndex) <= 0) {
          try {
            routeProjectedEndIndex = indexedRoute.indexOfAfter(routeProjectedEndCoord, routeIndex);
            if (Double.isNaN(routeProjectedEndIndex.getSegmentFraction())) {
              // can't go forward
              routeProjectedEndIndex = routeIndex; // this is bad, but not terrible
              // since we are advancing along the edge
            }
          } catch (AssertionFailedException e) {
            routeProjectedEndIndex = routeIndex;
          }
          routeProjectedEndCoord = routeProjectedEndIndex.getCoordinate(routeGeometry);
        }

        double positionError = distance(routeProjectedEndCoord, endCoord);
        double travelAlongRoute = distanceAlongGeometry(
          routeGeometry,
          routeIndex,
          routeProjectedEndIndex
        );
        double travelAlongEdge = distanceAlongGeometry(edgeGeometry, edgeIndex, newEdgeIndex);
        double travelError = Math.abs(travelAlongEdge - travelAlongRoute);

        double error = positionError + travelError;

        if (error > MAX_ERROR) {
          // we're not going to bother with states which are
          // totally wrong
          return nextStates;
        }

        for (Edge e : getOutgoingMatchableEdges(toVertex)) {
          double cost = error + NEW_SEGMENT_PENALTY;
          if (!carsCanTraverse(e)) {
            cost += NO_TRAVERSE_PENALTY;
          }
          MatchState nextState = new MidblockMatchState(
            this,
            routeGeometry,
            e,
            routeProjectedEndIndex,
            new LinearLocation(),
            cost,
            travelAlongRoute
          );
          nextStates.add(nextState);
        }
      } else {
        double travelAlongEdge = distanceAlongGeometry(edgeGeometry, edgeIndex, newEdgeIndex);
        double travelAlongRoute = distanceAlongGeometry(routeGeometry, routeIndex, routeSuccessor);
        double travelError = Math.abs(travelAlongRoute - travelAlongEdge);

        double positionError = distance(edgeCoord, newRouteCoord);

        double error = travelError + positionError;

        MatchState nextState = new MidblockMatchState(
          this,
          routeGeometry,
          edge,
          routeSuccessor,
          newEdgeIndex,
          error,
          travelAlongRoute
        );
        nextStates.add(nextState);

        // it's also possible that, although we have not yet reached the end of this edge,
        // we are going to turn, because the route turns earlier than the edge. In that
        // case, we jump to the corner, and our error is the distance from the route point
        // and the corner

        Vertex toVertex = edge.getToVertex();
        double travelAlongOldEdge = distanceAlongGeometry(edgeGeometry, edgeIndex, null);

        for (Edge e : getOutgoingMatchableEdges(toVertex)) {
          Geometry newEdgeGeometry = e.getGeometry();
          LocationIndexedLine newIndexedEdge = new LocationIndexedLine(newEdgeGeometry);
          newEdgeIndex = newIndexedEdge.project(newRouteCoord);
          Coordinate newEdgeCoord = newEdgeIndex.getCoordinate(newEdgeGeometry);
          positionError = distance(newEdgeCoord, newRouteCoord);
          travelAlongEdge =
            travelAlongOldEdge +
            distanceAlongGeometry(newEdgeGeometry, new LinearLocation(), newEdgeIndex);
          travelError = Math.abs(travelAlongRoute - travelAlongEdge);

          error = travelError + positionError;

          if (error > MAX_ERROR) {
            // we're not going to bother with states which are
            // totally wrong
            return nextStates;
          }

          double cost = error + NEW_SEGMENT_PENALTY;
          if (!carsCanTraverse(e)) {
            cost += NO_TRAVERSE_PENALTY;
          }

          nextState =
            new MidblockMatchState(
              this,
              routeGeometry,
              e,
              routeSuccessor,
              new LinearLocation(),
              cost,
              travelAlongRoute
            );
          nextStates.add(nextState);
        }
      }
      return nextStates;
    } else {
      Coordinate routeCoord = routeIndex.getCoordinate(routeGeometry);
      LinearLocation projected = indexedEdge.project(routeCoord);
      double locationError = distance(projected.getCoordinate(edgeGeometry), routeCoord);

      MatchState end = new EndMatchState(this, locationError, 0);
      return Arrays.asList(end);
    }
  }

  public int hashCode() {
    return (edge.hashCode() * 1337 + hashCode(edgeIndex)) * 1337 + hashCode(routeIndex);
  }

  public boolean equals(Object o) {
    if (!(o instanceof MidblockMatchState)) {
      return false;
    }
    MidblockMatchState other = (MidblockMatchState) o;
    return (
      other.edge == edge &&
      other.edgeIndex.compareTo(edgeIndex) == 0 &&
      other.routeIndex.compareTo(routeIndex) == 0
    );
  }

  public String toString() {
    return (
      "MidblockMatchState(" +
      edge +
      ", " +
      edgeIndex.getSegmentIndex() +
      ", " +
      edgeIndex.getSegmentFraction() +
      ") - " +
      currentError
    );
  }

  private int hashCode(LinearLocation location) {
    return (
      location.getComponentIndex() *
      1000000 +
      location.getSegmentIndex() *
      37 +
      Double.valueOf(location.getSegmentFraction()).hashCode()
    );
  }
}
