package org.opentripplanner.routing.graphfinder;

import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.List;

// TODO Seems like this should be merged with the PlaceFinderTraverseVisitor
/**
 * A TraverseVisitor used in finding stops while walking the street graph.
 */
public class StopFinderTraverseVisitor implements TraverseVisitor {

  private final double radiusMeters;

  public StopFinderTraverseVisitor(double radiusMeters) {
    this.radiusMeters = radiusMeters;
  }

  /** A list of closest stops found while walking the graph */
  public final List<NearbyStop> stopsFound = new ArrayList<>();

  @Override
  public void visitEdge(Edge edge, State state) { }

  @Override
  public void visitEnqueue(State state) { }

  // Accumulate stops into ret as the search runs.
  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    if (vertex instanceof TransitStopVertex) {
      stopsFound.add(NearbyStop.nearbyStopForState(state, ((TransitStopVertex) vertex).getStop()));
    }
  }

  /**
   * @return A SkipEdgeStrategy that will stop exploring edges after the distance radius has been
   *          reached.
   */
  public SkipEdgeStrategy getSkipEdgeStrategy() {

    return (origin, target, current, edge, spt, traverseOptions) ->
        current.getWalkDistance() > radiusMeters;
  }
}
