package org.opentripplanner.ext.flex.flexpathcalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
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

  private final Supplier<List<Edge>> edgeSupplier;
  private final double effectiveWalkDistance;
  private List<Edge> edges;

  private FlexGraphPathAdapter(Supplier<List<Edge>> edgeSupplier, double effectiveWalkDistance) {
    this.edgeSupplier = edgeSupplier;
    this.effectiveWalkDistance = effectiveWalkDistance;
  }

  /**
   * Walk the state chain and collect edges in chronological order (origin → destination), summing
   * up the effective walk distance along the way.
   */
  public static FlexGraphPathAdapter of(State state) {
    double distance = 0.0;
    var edges = new ArrayList<Edge>();

    for (var backEdge : state.listBackEdges()) {
      distance += backEdge.getDistanceMeters();
      edges.add(backEdge);
    }

    Supplier<List<Edge>> edgeSupplier = () -> edges;
    // For depart-after, edges are in reverse chronological order; reverse to chronological.
    // For arriveBy, the A* searched backward so edges are already chronological.
    if (!state.getRequest().arriveBy()) {
      edgeSupplier = () -> {
        Collections.reverse(edges);
        return edges;
      };
    }

    return new FlexGraphPathAdapter(edgeSupplier, distance);
  }

  public List<Edge> edges() {
    if (edges == null) {
      edges = edgeSupplier.get();
    }
    return edges;
  }

  public double distance() {
    return effectiveWalkDistance;
  }
}
