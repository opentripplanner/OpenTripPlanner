package org.opentripplanner.routing.graphfinder;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

// TODO Seems like this should be merged with the PlaceFinderTraverseVisitor

/**
 * A TraverseVisitor used in finding stops while walking the street graph.
 */
public class StopFinderTraverseVisitor implements TraverseVisitor<State, Edge> {

  private final double radiusMeters;
  /** A list of closest stops found while walking the graph */
  public final List<NearbyStop> stopsFound = new ArrayList<>();

  public StopFinderTraverseVisitor(double radiusMeters) {
    this.radiusMeters = radiusMeters;
  }

  @Override
  public void visitEdge(Edge edge) {}

  // Accumulate stops into ret as the search runs.
  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    if (vertex instanceof TransitStopVertex) {
      stopsFound.add(NearbyStop.nearbyStopForState(state, ((TransitStopVertex) vertex).getStop()));
    }
  }

  @Override
  public void visitEnqueue() {}

  /**
   * @return A SkipEdgeStrategy that will stop exploring edges after the distance radius has been
   * reached.
   */
  public SkipEdgeStrategy<State, Edge> getSkipEdgeStrategy() {
    return (current, edge) -> current.getWalkDistance() > radiusMeters;
  }
}
