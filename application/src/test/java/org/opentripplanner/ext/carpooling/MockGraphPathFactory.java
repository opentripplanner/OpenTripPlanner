package org.opentripplanner.ext.carpooling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Factory for creating mock GraphPath objects for testing.
 */
public class MockGraphPathFactory {

  /**
   * Creates a mock GraphPath with default 5-minute duration.
   */
  public static GraphPath<State, Edge, Vertex> createMockGraphPath() {
    return createMockGraphPath(Duration.ofMinutes(5));
  }

  /**
   * Creates a mock GraphPath with specified duration.
   */
  @SuppressWarnings("unchecked")
  public static GraphPath<State, Edge, Vertex> createMockGraphPath(Duration duration) {
    var mockPath = (GraphPath<State, Edge, Vertex>) mock(GraphPath.class);

    // Set public fields directly instead of stubbing to avoid Mockito state issues
    mockPath.states = new java.util.LinkedList<>(createMockStates(duration));
    mockPath.edges = new java.util.LinkedList<Edge>();

    return mockPath;
  }

  /**
   * Creates mock State objects with specified time duration.
   */
  private static List<State> createMockStates(Duration duration) {
    var startState = mock(State.class);
    var endState = mock(State.class);

    var startTime = Instant.now();
    var endTime = startTime.plus(duration);

    when(startState.getTime()).thenReturn(startTime);
    when(endState.getTime()).thenReturn(endTime);

    var mockVertex = mock(Vertex.class);
    when(startState.getVertex()).thenReturn(mockVertex);
    when(endState.getVertex()).thenReturn(mockVertex);

    return List.of(startState, endState);
  }

  /**
   * Creates multiple mock GraphPaths with varying durations.
   */
  public static List<GraphPath<State, Edge, Vertex>> createMockGraphPaths(int count) {
    return java.util.stream.IntStream.range(0, count)
      .mapToObj(i -> createMockGraphPath(Duration.ofMinutes(5 + i)))
      .toList();
  }
}
