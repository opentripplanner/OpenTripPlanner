package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class GraphPathUtils {

  /**
   * Calculates cumulative durations from pre-routed segments.
   */
  public static Duration[] calculateCumulativeDurations(GraphPath<State, Edge, Vertex>[] segments) {
    Duration[] cumulativeDurations = new Duration[segments.length + 1];
    cumulativeDurations[0] = Duration.ZERO;

    for (int i = 0; i < segments.length; i++) {
      Duration segmentDuration = Duration.between(
        segments[i].states.getFirst().getTime(),
        segments[i].states.getLast().getTime()
      );
      cumulativeDurations[i + 1] = cumulativeDurations[i].plus(segmentDuration);
    }

    return cumulativeDurations;
  }
}
