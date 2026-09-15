package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * A street segment of a driver's route — the drive between two consecutive waypoints of the
 * (possibly modified) route — as routed by a {@link CarpoolRouter}.
 * <p>
 * Insertion evaluation needs only a segment's travel time: the delay constraints are arithmetic
 * on segment durations, and the best insertion is chosen on total trip duration. The street path
 * itself (the chain of states and edges) is needed only for the few insertions that end up in an
 * itinerary, where it supplies the leg geometry and the pickup/dropoff places. A segment therefore
 * exposes its {@link #durationSeconds() duration} eagerly and materialises its {@link #path()} on
 * demand, so evaluating thousands of candidate insertions per request does not build thousands of
 * paths that are thrown away.
 */
public interface RoutedSegment {
  /** The vertex the segment departs from. */
  Vertex from();

  /** The vertex the segment arrives at. */
  Vertex to();

  /**
   * Travel time in whole seconds — the same value {@code path().getDuration()} reports, i.e. the
   * elapsed time of the arrival state.
   */
  int durationSeconds();

  /** {@link #durationSeconds()} as a {@link Duration}. */
  default Duration duration() {
    return Duration.ofSeconds(durationSeconds());
  }

  /**
   * The street path of this segment in chronological order. Implementations may build it on the
   * first call and memoise it; call it only when the path is actually needed. Never {@code null}.
   */
  GraphPath<State, Edge, Vertex> path();

  /**
   * Makes the segment independent of the search structure it was answered from, so that structure
   * can be released while {@link #path()} stays available. Cheap: a tree-backed segment keeps its
   * edge chain, not a path. A segment that already owns its path has nothing to do.
   */
  default void detach() {}

  /** Wraps an already materialised path. */
  static RoutedSegment of(GraphPath<State, Edge, Vertex> path) {
    return new PathSegment(path);
  }

  /**
   * Cumulative durations along a chain of segments, with {@code stopDuration} added at every
   * intermediate point — see {@link GraphPathUtils#calculateCumulativeDurations(Duration[],
   * Duration)} for the exact rule.
   */
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
