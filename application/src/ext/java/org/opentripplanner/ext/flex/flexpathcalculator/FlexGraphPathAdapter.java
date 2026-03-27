package org.opentripplanner.ext.flex.flexpathcalculator;

import java.util.function.Supplier;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

/**
 * Extracts the traversed edges and effective walk distance from a {@link State} chain produced by
 * an A* street search. The state chain is a linked list from the final state back to the origin via
 * {@link State#getBackState()}/{@link State#getBackEdge()}.
 * <p>
 * This utility encapsulates the direction-dependent ordering: for depart-after searches the chain
 * yields edges in reverse chronological order (newest first), while for arriveBy searches the chain
 * already yields edges in chronological order.
 * Implementation note: an earlier design relied on {@link org.opentripplanner.astar.model.GraphPath}
 * to extract the list of edges in chronological order / reverse chronological order.
 * The current implementation is optimized for reducing memory allocation.
 */
public class FlexGraphPathAdapter {

  private final Supplier<LineString> geometrySupplier;
  private final double effectiveWalkDistance;

  /**
   * @param geometrySupplier Reversing the edges is expensive, so we only do it if we need it.
   * @param distance
   */
  private FlexGraphPathAdapter(Supplier<LineString> geometrySupplier, double distance) {
    this.geometrySupplier = geometrySupplier;
    this.effectiveWalkDistance = distance;
  }

  /**
   * Walk the state chain and collect edges in chronological order (origin → destination), summing
   * up the effective walk distance along the way.
   */
  public static FlexGraphPathAdapter of(State state) {
    double distance = 0.0;

    for (var backEdge : state.listBackEdges()) {
      distance += backEdge.getDistanceMeters();
    }

    // For depart-after, edges are in reverse chronological order; reverse to chronological.
    // For arriveBy, the A* searched backward so edges are already chronological.
    Supplier<LineString> edgeSupplier = () -> {
      var linestring = GeometryUtils.concatenateLineStrings(
        state.listBackEdges(),
        Edge::getGeometry
      );
      if (!state.getRequest().arriveBy()) {
        return linestring.reverse();
      } else {
        return linestring;
      }
    };

    return new FlexGraphPathAdapter(edgeSupplier, distance);
  }

  public Supplier<LineString> geometry() {
    return geometrySupplier;
  }

  public double distance() {
    return effectiveWalkDistance;
  }
}
