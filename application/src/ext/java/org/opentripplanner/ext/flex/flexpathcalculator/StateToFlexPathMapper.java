package org.opentripplanner.ext.flex.flexpathcalculator;

import java.util.function.Supplier;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
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
  static FlexPath map(State state) {
    double distance_m = 0.0;

    // TODO: compute this during traversal of the edges in a follow up PR
    for (var backEdge : state.listBackEdges()) {
      distance_m += backEdge.getDistanceMeters();
    }

    // computing the linestring from the graph path is a surprisingly expensive operation
    // so we delay it until it's actually needed. since most flex paths are never shown to the user
    // this improves performance quite a bit.

    Supplier<LineString> geometrySupplier = () -> {
      // For depart-after, edges are in reverse chronological order; reverse to chronological.
      // For arriveBy, the A* searched backward so edges are already chronological.
      var linestring = GeometryUtils.concatenateLineStrings(
        state.listBackEdges(),
        Edge::getGeometry
      );
      if (state.getRequest().arriveBy()) {
        return linestring;
      } else {
        return linestring.reverse();
      }
    };

    return new FlexPath((int) distance_m, (int) state.getElapsedTimeSeconds(), geometrySupplier);
  }
}
