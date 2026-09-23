package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * The drive between two consecutive waypoints of a (possibly modified) driver route, as routed by
 * a {@link CarpoolRouter}.
 * <p>
 * Insertion evaluation needs only the duration; the street path is needed for the few insertions
 * that end up in an itinerary. A segment therefore knows its {@link #durationSeconds() duration}
 * from the start and builds its {@link #path()} on demand.
 */
public interface RoutedSegment {
  Vertex from();

  Vertex to();

  /** Travel time in whole seconds, the same value {@code path().getDuration()} reports. */
  int durationSeconds();

  default Duration duration() {
    return Duration.ofSeconds(durationSeconds());
  }

  /** The street path in chronological order, built on the first call. Never {@code null}. */
  GraphPath<State, Edge, Vertex> path();

  /**
   * Makes the segment independent of the search structure it was answered from, so that structure
   * can be garbage collected while {@link #path()} stays available. Cheap: a tree-backed segment
   * keeps its edge chain, not a path.
   */
  default void detach() {}

  /** Wraps an already materialised path. */
  static RoutedSegment of(GraphPath<State, Edge, Vertex> path) {
    return new PathSegment(path);
  }

  /** Cumulative durations along a chain of segments, with {@code stopDuration} added at every intermediate point. */
  static Duration[] cumulativeDurations(
    List<? extends RoutedSegment> segments,
    Duration stopDuration
  ) {
    Duration[] durations = new Duration[segments.size()];
    for (int i = 0; i < durations.length; i++) {
      durations[i] = segments.get(i).duration();
    }
    return GraphPathUtils.calculateCumulativeDurations(durations, stopDuration);
  }
}
