package org.opentripplanner.place.api;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.nearbystopfinder.ChronologicalGraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

/**
 * A specific stop at a distance. Also includes a geometry and potentially a list of edges and a
 * state of how to reach the stop from the search origin
 */
public class NearbyStop implements Comparable<NearbyStop> {

  public final FeedScopedId stopId;
  public final double distance;

  /**
   * All edges that are needed to reach the stop.
   */
  public final List<Edge> edges;

  /**
   * For access, this is a list of states starting from origin to the access stop split at via
   * locations visited inside the access. For egress, this is a list starting at the egress stop
   * ending at the destination split at the via locations visited inside the egress.
   */
  public final List<State> finalStates;

  public NearbyStop(FeedScopedId stopId, double distance, List<Edge> edges, State lastState) {
    this(stopId, distance, edges, List.of(Objects.requireNonNull(lastState)));
  }

  public NearbyStop(
    FeedScopedId stopId,
    double distance,
    List<Edge> edges,
    List<State> finalStates
  ) {
    this.stopId = Objects.requireNonNull(stopId);
    this.distance = distance;
    this.edges = edges;
    this.finalStates = finalStates;
  }

  /**
   * Given a State at a StopVertex, bundle the stop's id together with information about how far
   * away it is and the geometry of the path leading up to the given State.
   */
  public static NearbyStop nearbyStopForState(State state, FeedScopedId stopId) {
    var result = ChronologicalGraphPath.of(state);
    return new NearbyStop(stopId, result.effectiveWalkDistance(), result.edges(), state);
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
    var weightDifference = (int) (this.weight()) - (int) (that.weight());
    if (weightDifference != 0) {
      return weightDifference;
    }
    return (int) (this.distance) - (int) (that.distance);
  }

  /**
   * Duration it took to reach the stop.
   */
  public Duration duration() {
    return Duration.ofSeconds(
      finalStates.stream().mapToLong(State::getElapsedTimeSeconds).reduce(0, Long::sum)
    );
  }

  /**
   * Weight (cost) of reaching the stop.
   */
  public double weight() {
    return finalStates.stream().mapToDouble(State::getWeight).reduce(0.0, Double::sum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stopId, distance, edges, finalStates);
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
      Objects.equals(finalStates, that.finalStates)
    );
  }

  public String toString() {
    return String.format(
      Locale.ROOT,
      "stop %s at %.1f meters%s%s",
      stopId,
      distance,
      " (" + edges.size() + " edges)",
      " (" + finalStates.size() + " finalStates)"
    );
  }
}
