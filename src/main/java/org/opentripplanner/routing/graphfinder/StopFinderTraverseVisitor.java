package org.opentripplanner.routing.graphfinder;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * A TraverseVisitor used in finding stops while walking the street graph.
 */
public class StopFinderTraverseVisitor implements TraverseVisitor<State, Edge> {

  private final double radiusMeters;

  /** A list of closest stops found while walking the graph */
  private final List<NearbyStop> stopsFound = new ArrayList<>();

  public StopFinderTraverseVisitor(double radiusMeters) {
    this.radiusMeters = radiusMeters;
  }

  @Override
  public void visitEdge(Edge edge) {}

  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    if (vertex instanceof TransitStopVertex tsv) {
      stopsFound.add(NearbyStop.nearbyStopForState(state, tsv.getStop()));
    }
  }

  @Override
  public void visitEnqueue() {}

  /**
   * @return A de-duplicated list of nearby stops found by this visitor.
   */
  public List<NearbyStop> stopsFound() {
    return ListUtils.distinctByKey(stopsFound, ns -> ns.stop);
  }

  /**
   * @return A SkipEdgeStrategy that will stop exploring edges after the distance radius has been
   * reached.
   */
  public SkipEdgeStrategy<State, Edge> getSkipEdgeStrategy() {
    return (current, edge) -> current.getWalkDistance() > radiusMeters;
  }
}
