package org.opentripplanner.routing.graphfinder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A specific stop at a distance. Also includes a geometry and potentially a list of edges and a
 * state of how to reach the stop from the search origin
 */
public class NearbyStop implements Comparable<NearbyStop> {

  public final StopLocation stop;
  public final double distance;

  /**
   * All edges that are needed to reach the stop.
   */
  public final List<Edge> edges;

  /**
   * This a list of states where the last state in the list is always at the stop and states before
   * that state end at a via location.
   */
  public final List<State> lastStates;

  public NearbyStop(StopLocation stop, double distance, List<Edge> edges, List<State> lastStates) {
    this.stop = Objects.requireNonNull(stop);
    this.distance = distance;
    this.edges = edges;
    this.lastStates = lastStates;
  }

  public NearbyStop(StopLocation stop, double distance, List<Edge> edges, State lastState) {
    this(stop, distance, edges, List.of(Objects.requireNonNull(lastState)));
  }

  /**
   * Given a State at a StopVertex, bundle the StopVertex together with information about how far
   * away it is and the geometry of the path leading up to the given State.
   */
  public static NearbyStop nearbyStopForState(State state, StopLocation stop) {
    double effectiveWalkDistance = 0.0;
    var graphPath = new GraphPath<>(state);
    var edges = new ArrayList<Edge>();
    for (Edge edge : graphPath.edges) {
      effectiveWalkDistance += edge.getEffectiveWalkDistance();
      edges.add(edge);
    }
    return new NearbyStop(stop, effectiveWalkDistance, edges, state);
  }

  /**
   * Create a NearbyStop with zero distance and no edges.
   */
  public static NearbyStop ofZeroDistance(StopLocation stop, State state) {
    return new NearbyStop(stop, 0d, Collections.emptyList(), state);
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
      lastStates.stream().mapToLong(State::getElapsedTimeSeconds).reduce(0, Long::sum)
    );
  }

  /**
   * Weight (cost) of reaching the stop.
   */
  public double weight() {
    return lastStates.stream().mapToDouble(State::getWeight).reduce(0.0, Double::sum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stop, distance, edges, lastStates);
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
      stop.equals(that.stop) &&
      Objects.equals(edges, that.edges) &&
      Objects.equals(lastStates, that.lastStates)
    );
  }

  public String toString() {
    return String.format(
      Locale.ROOT,
      "stop %s at %.1f meters%s%s",
      stop,
      distance,
      " (" + edges.size() + " edges)",
      " (" + lastStates.size() + " lastStates)"
    );
  }
}
