package org.opentripplanner.routing;

import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.List;

public class StopFinder {

  /* TODO: an almost similar function exists in ProfileRouter, combine these.
   *  Should these live in a separate class? */
  public static List<StopAndDistance> findClosestStopsByWalking(
      Graph graph, double lat, double lon, int radius
  ) {
    // Make a normal OTP routing request so we can traverse edges and use GenericAStar
    // TODO make a function that builds normal routing requests from profile requests
    RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
    rr.from = new GenericLocation(null, null, lat, lon);
    // FIXME requires destination to be set, not necessary for analyst
    rr.to = new GenericLocation(null, null, lat, lon);
    rr.oneToMany = true;
    rr.setRoutingContext(graph);
    rr.walkSpeed = 1;
    rr.dominanceFunction = new DominanceFunction.LeastWalk();
    // RR dateTime defaults to currentTime.
    // If elapsed time is not capped, searches are very slow.
    rr.worstTime = (rr.dateTime + radius);
    AStar astar = new AStar();
    rr.setNumItineraries(1);
    StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor();
    astar.setTraverseVisitor(visitor);
    astar.getShortestPathTree(rr, 1); // timeout in seconds
    // Destroy the routing context, to clean up the temporary edges & vertices
    rr.rctx.destroy();
    return visitor.stopsFound;
  }

  public static class StopAndDistance {

    public Stop stop;
    public int distance;

    public StopAndDistance(Stop stop, int distance) {
      this.stop = stop;
      this.distance = distance;
    }
  }

  static private class StopFinderTraverseVisitor implements TraverseVisitor {

    List<StopAndDistance> stopsFound = new ArrayList<>();

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
}
