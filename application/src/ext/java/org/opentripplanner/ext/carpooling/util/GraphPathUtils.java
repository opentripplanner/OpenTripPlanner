package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public final class GraphPathUtils {

  private GraphPathUtils() {}

  /**
   * Returns the duration of the given path, or {@link Duration#ZERO} if the path is {@code null}.
   */
  public static Duration durationOrZero(@Nullable GraphPath<State, Edge, Vertex> path) {
    return path == null ? Duration.ZERO : Duration.ofSeconds(path.getDuration());
  }

  /**
   * Returns the A* weight of the given path, or {@code 0} if the path is {@code null}. The weight
   * already accounts for the user's walk preferences (reluctance, safety factor, slope cost,
   * etc.) since it comes from the search that produced the path.
   */
  public static double weightOrZero(@Nullable GraphPath<State, Edge, Vertex> path) {
    return path == null ? 0 : path.getWeight();
  }

  /**
   * Calculates cumulative durations from pre-routed segments, including stop duration
   * at each intermediate stop.
   *
   * @param segments Pre-routed segments
   * @param stopDuration Duration added at each intermediate stop
   */
  public static Duration[] calculateCumulativeDurations(
    GraphPath<State, Edge, Vertex>[] segments,
    Duration stopDuration
  ) {
    Duration[] segmentDurations = new Duration[segments.length];
    for (int i = 0; i < segments.length; i++) {
      segmentDurations[i] = Duration.ofSeconds(segments[i].getDuration());
    }
    return calculateCumulativeDurations(segmentDurations, stopDuration);
  }

  /**
   * Calculates cumulative arrival times from segment durations, including a stop delay
   * at each intermediate point. The stop delay is added <em>before</em> each segment
   * except the first, modelling time spent at an intermediate stop before departing
   * to the next point. No delay is added at the origin (before segment 0) or after the
   * final segment.
   * <p>
   * Given N segments, the result has N+1 entries:
   * <pre>
   *   result[0] = 0                                           (origin)
   *   result[1] = segment[0]                                  (no preceding stop delay)
   *   result[2] = segment[0] + stopDuration + segment[1]
   *   result[k] = result[k-1] + stopDuration + segment[k-1]  (for k >= 2)
   * </pre>
   * Example: 6 segments of 10 min each, 1 min stop duration:
   * {@code [0, 10, 21, 32, 43, 54, 65]}
   *
   * @param segmentDurations Duration of each segment
   * @param stopDuration Duration added before each segment except the first
   * @return Array of cumulative durations with length segmentDurations.length + 1
   */
  public static Duration[] calculateCumulativeDurations(
    Duration[] segmentDurations,
    Duration stopDuration
  ) {
    Duration[] cumulativeDurations = new Duration[segmentDurations.length + 1];
    cumulativeDurations[0] = Duration.ZERO;

    for (int i = 0; i < segmentDurations.length; i++) {
      Duration stopDelay = i > 0 ? stopDuration : Duration.ZERO;
      cumulativeDurations[i + 1] = cumulativeDurations[i].plus(stopDelay).plus(segmentDurations[i]);
    }

    return cumulativeDurations;
  }
}
