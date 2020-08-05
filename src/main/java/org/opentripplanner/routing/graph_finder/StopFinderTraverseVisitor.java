package org.opentripplanner.routing.graph_finder;

import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.List;

public class StopFinderTraverseVisitor implements TraverseVisitor {

  public final List<StopAndDistance> stopsFound = new ArrayList<>();

  @Override
  public void visitEdge(Edge edge, State state) { }

  @Override
  public void visitEnqueue(State state) { }

  // Accumulate stops into ret as the search runs.
  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    if (vertex instanceof TransitStopVertex) {
      stopsFound.add(new StopAndDistance(((TransitStopVertex) vertex).getStop(),
          (int) state.getElapsedTimeSeconds()
      ));
    }
  }
}
