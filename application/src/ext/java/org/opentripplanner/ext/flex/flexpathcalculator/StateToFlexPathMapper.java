package org.opentripplanner.ext.flex.flexpathcalculator;

import java.util.function.Supplier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Extracts the geometry and distance from a {@link State} chain produced by
 * an A* street search. The state chain is a linked list from the final state back to the origin via
 * {@link State#getBackState()}/{@link State#getBackEdge()}.
 * <p>
 * This utility encapsulates the direction-dependent ordering: for depart-after searches the chain
 * yields a geometry in reverse chronological order (newest first), while for arriveBy searches the chain
 * already yields edges in chronological order.
 * Implementation note: an earlier design relied on {@link org.opentripplanner.astar.model.GraphPath}
 * to extract the list of edges in chronological order / reverse chronological order.
 * The current implementation is optimized for reducing memory allocation and CPU usage.
 */
class StateToFlexPathMapper {

  /**
   * Walk the state chain and collect edges in chronological order (origin → destination), summing
   * up the distance along the way.
   */
  static FlexPath map(State state, Vertex origin, Vertex destination) {
    // computing the linestring from the graph path is a surprisingly expensive operation
    // so we delay it until it's actually needed. since most flex paths are never shown to the user
    // this improves performance quite a bit.
    Supplier<LineString> geometrySupplier = () -> {
      return bezierCurve(origin.getCoordinate(), destination.getCoordinate());
    };

    return new FlexPath(
      (int) state.getTraversalDistanceMeters(),
      (int) state.getElapsedTimeSeconds(),
      geometrySupplier
    );
  }

  private static LineString bezierCurve(Coordinate start, Coordinate end) {
    double heightFactor = 0.05; // Adjust for more/less curve
    double midX = (start.x + end.x) / 2;
    double midY = (start.y + end.y) / 2;
    Coordinate control = new Coordinate(midX, midY + heightFactor);

    // Create a Bezier curve by densifying the curve
    int numPoints = 20;
    double[] curvePoints = new double[numPoints * 2];

    for (int i = 0; i < numPoints; i++) {
      double t = (double) i / (numPoints - 1);
      // Quadratic Bezier interpolation
      double x = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x;
      double y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y;
      curvePoints[i * 2] = x;
      curvePoints[(i * 2) + 1] = y;
    }

    return GeometryUtils.makeLineString(curvePoints);
  }
}
