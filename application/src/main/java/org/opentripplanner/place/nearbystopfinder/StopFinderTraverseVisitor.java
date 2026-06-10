package org.opentripplanner.place.nearbystopfinder;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * A TraverseVisitor used in finding stops while walking the street graph.
 */
class StopFinderTraverseVisitor implements TraverseVisitor<State, Edge> {

  private final double radiusMeters;

  /** A list of closest stops found while walking the graph */
  private final List<NearbyStop> stopsFound = new ArrayList<>();

  StopFinderTraverseVisitor(double radiusMeters) {
    this.radiusMeters = radiusMeters;
  }

  @Override
  public void visitEdge(Edge edge) {}

  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    if (vertex instanceof TransitStopVertex tsv) {
      stopsFound.add(NearbyStop.nearbyStopForState(state, tsv.getId()));
    }
  }

  @Override
  public void visitEnqueue() {}

  /**
   * @return A de-duplicated list of nearby stops found by this visitor.
   */
  List<NearbyStop> stopsFound() {
    return ListUtils.distinctByKey(stopsFound, ns -> ns.stopId);
  }

  /**
   * @return A SkipEdgeStrategy that will stop exploring edges after the distance radius has been
   * reached.
   */
  SkipEdgeStrategy<State, Edge> getSkipEdgeStrategy() {
    return (current, edge) -> current.getTraversalDistanceMeters() > radiusMeters;
  }
}
