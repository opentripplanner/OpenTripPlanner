package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

/**
 * Builder for creating GraphPath objects for carpooling tests using real State chains.
 * This replaces MockGraphPathFactory with OTP's preferred TestStateBuilder pattern.
 */
public class CarpoolGraphPathBuilder {

  // Walking speed in m/s (OTP default from WalkPreferences)
  private static final double WALKING_SPEED_MPS = 1.33;

  // Default number of edges to distribute duration across
  private static final int DEFAULT_NUM_EDGES = 3;

  /**
   * Creates a GraphPath with default 5-minute duration.
   */
  public static GraphPath<State, Edge, Vertex> createGraphPath() {
    return createGraphPath(Duration.ofMinutes(5));
  }

  /**
   * Creates a GraphPath with specified duration using State chain.
   *
   * @param duration Total duration for the path
   * @return GraphPath with real State objects and accurate timing
   */
  public static GraphPath<State, Edge, Vertex> createGraphPath(Duration duration) {
    var builder = TestStateBuilder.ofWalking();

    // Calculate distance needed for target duration
    double totalDistanceMeters = duration.toSeconds() * WALKING_SPEED_MPS;

    // Distribute across multiple edges for realistic path
    int numEdges = DEFAULT_NUM_EDGES;
    int distancePerEdge = (int) Math.ceil(totalDistanceMeters / numEdges);

    // Build state chain with calculated distances
    for (int i = 0; i < numEdges; i++) {
      builder.streetEdge("segment-" + i, distancePerEdge);
    }

    return new GraphPath<>(builder.build());
  }

  /**
   * Creates multiple GraphPaths with varying durations.
   * Each path has duration = 5 minutes + index minutes.
   *
   * @param count Number of paths to create
   * @return List of GraphPaths with incrementing durations
   */
  public static List<GraphPath<State, Edge, Vertex>> createGraphPaths(int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> createGraphPath(Duration.ofMinutes(5 + i)))
      .toList();
  }
}
