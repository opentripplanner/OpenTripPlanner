package org.opentripplanner.street.model.path;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class StreetPathSegment {

  private final List<State> states;
  private final List<Edge> edges;

  public StreetPathSegment(GraphPath<State, Edge, Vertex> path) {
    this(path.states, path.edges);
  }

  public StreetPathSegment(List<State> states, List<Edge> edges) {
    if (states.isEmpty()) {
      throw new IllegalArgumentException("A path needs at least one state");
    }
    if (edges.size() != states.size() - 1) {
      throw new IllegalArgumentException("A path needs an edge between each state");
    }
    this.states = states;
    this.edges = edges;
  }

  public StreetPathSegment(State endState) {
    this(new GraphPath<>(endState));
  }

  /// The start of the path in seconds
  public Instant startTime() {
    return states.getFirst().getTime();
  }

  /// The end of the path in seconds
  public Instant endTime() {
    return states.getLast().getTime();
  }

  /// The start of the path in milliseconds
  public Instant startTimeAccurate() {
    return states.getFirst().getTimeAccurate();
  }

  /// The end of the path in milliseconds
  public Instant endTimeAccurate() {
    return states.getLast().getTimeAccurate();
  }

  public double weight() {
    return states.getLast().weight - states.getFirst().weight;
  }

  public double distanceMeters() {
    return edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
  }

  /// The duration of the trip in seconds
  public Duration duration() {
    return startTime().until(endTime());
  }

  public LineString geometry() {
    var geometries = edges
      .stream()
      .filter(Edge::includeGeometryInPath)
      .map(Edge::getGeometry)
      .filter(Objects::nonNull);

    return GeometryUtils.concatenateLineStrings(geometries::iterator);
  }

  /// Get all the states of this path
  public List<State> states() {
    return states;
  }

  /// Get the last state in the path
  public State lastState() {
    return states.getLast();
  }

  /// Get all the states of this path
  public List<Edge> edges() {
    return edges;
  }

  /// Get a specific section of this path as a new path.
  ///
  /// @param startIdx the first state index (inclusive)
  /// @param endIdx the end state index (exclusive)
  public StreetPathSegment subSegment(int startIdx, int endIdx) {
    var subStates = states.subList(startIdx, endIdx);
    var subEdges = edges.subList(startIdx, endIdx - 1);
    return new StreetPathSegment(subStates, subEdges);
  }

  /// This is only used in the carpooling code and can be removed once the carpooling migrates
  /// to use the StreetPath instead
  @Deprecated
  public GraphPath<State, Edge, Vertex> toGraphPath() {
    return new GraphPath<>(states, edges);
  }

  double finalWeight() {
    return states.getLast().weight;
  }
}
