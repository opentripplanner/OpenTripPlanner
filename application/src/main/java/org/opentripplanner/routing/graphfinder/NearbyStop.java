package org.opentripplanner.routing.graphfinder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

/**
 * A specific stop at a distance. Also includes a geometry and potentially a list of edges and a
 * state of how to reach the stop from the search origin
 */
public class NearbyStop implements Comparable<NearbyStop> {

  public final FeedScopedId stopId;
  public final double distance;

  public final List<Edge> edges;
  public final State state;

  public NearbyStop(FeedScopedId stopId, double distance, List<Edge> edges, State state) {
    this.stopId = Objects.requireNonNull(stopId);
    this.distance = distance;
    this.edges = edges;
    this.state = state;
  }

  /**
   * Given a State at a StopVertex, bundle the stop's id together with information about how far
   * away it is and the geometry of the path leading up to the given State.
   */
  public static NearbyStop nearbyStopForState(State state, FeedScopedId stopId) {
    double effectiveWalkDistance = 0.0;
    var graphPath = new GraphPath<>(state);
    var edges = new ArrayList<Edge>();
    for (Edge edge : graphPath.edges) {
      effectiveWalkDistance += edge.getEffectiveWalkDistance();
      edges.add(edge);
    }
    return new NearbyStop(stopId, effectiveWalkDistance, edges, state);
  }

  /**
   * Create a NearbyStop with zero distance and no edges.
   */
  public static NearbyStop ofZeroDistance(FeedScopedId stopId, State state) {
    return new NearbyStop(stopId, 0d, Collections.emptyList(), state);
  }

  /**
   * Return {@code true} if this instance has a lower weight/cost than the given {@code other}.
   * If the state is not set, the distance is used for comparison instead. If the
   * weight/cost/distance is equals (or worse) this method returns {@code false}.
   */
  public boolean isBetter(NearbyStop other) {
    return compareTo(other) < 0;
  }

  @Override
  public int compareTo(NearbyStop that) {
    if ((this.state == null) != (that.state == null)) {
      throw new IllegalStateException(
        "Only NearbyStops which both contain or lack a state may be compared."
      );
    }

    if (this.state != null) {
      return (int) (this.state.getWeight()) - (int) (that.state.getWeight());
    }
    return (int) (this.distance) - (int) (that.distance);
  }

  /**
   * Duration it took to reach the stop.
   */
  public Duration duration() {
    return Duration.ofSeconds(state.getElapsedTimeSeconds());
  }

  @Override
  public int hashCode() {
    return Objects.hash(stopId, distance, edges, state);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final NearbyStop that = (NearbyStop) o;
    return (
      Double.compare(that.distance, distance) == 0 &&
      stopId.equals(that.stopId) &&
      Objects.equals(edges, that.edges) &&
      Objects.equals(state, that.state)
    );
  }

  public String toString() {
    return String.format(
      Locale.ROOT,
      "stop %s at %.1f meters%s%s",
      stopId,
      distance,
      edges != null ? " (" + edges.size() + " edges)" : "",
      state != null ? " w/state" : ""
    );
  }
}
