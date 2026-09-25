package org.opentripplanner.ext.carpooling.routing;

import java.util.Objects;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/** A {@link RoutedSegment} over a path that already exists, such as the result of a goal-directed search. */
final class PathSegment implements RoutedSegment {

  private final GraphPath<State, Edge, Vertex> path;

  PathSegment(GraphPath<State, Edge, Vertex> path) {
    this.path = Objects.requireNonNull(path, "path");
    if (path.states.isEmpty()) {
      throw new IllegalArgumentException("A routed segment needs a path with at least one state");
    }
  }

  @Override
  public Vertex from() {
    return path.states.getFirst().getVertex();
  }

  @Override
  public Vertex to() {
    return path.states.getLast().getVertex();
  }

  @Override
  public int durationSeconds() {
    return path.getDuration();
  }

  @Override
  public GraphPath<State, Edge, Vertex> path() {
    return path;
  }

  @Override
  public String toString() {
    return "PathSegment{" + from() + " -> " + to() + ", " + durationSeconds() + "s}";
  }
}
