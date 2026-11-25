package org.opentripplanner.ext.carpooling.street;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/** The car search that builds a shortest-path tree. */
public final class CarTreeSearch {

  private CarTreeSearch() {}

  /**
   * A car shortest-path tree from {@code root}, or to it when {@code reverse}. Nothing is expanded
   * past {@code searchLimit}, nor where {@code alsoSkip}, if given, skips.
   */
  public static ShortestPathTree<State, Edge, Vertex> carTree(
    Vertex root,
    boolean reverse,
    Duration searchLimit,
    @Nullable SkipEdgeStrategy<State, Edge> alsoSkip
  ) {
    var streetSearchRequest = reverse
      ? StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(true).build()
      : StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    SkipEdgeStrategy<State, Edge> skipEdgeStrategy = new DurationSkipEdgeStrategy<>(searchLimit);
    if (alsoSkip != null) {
      skipEdgeStrategy = new ComposingSkipEdgeStrategy<>(skipEdgeStrategy, alsoSkip);
    }
    var builder = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withSkipEdgeStrategy(skipEdgeStrategy)
      .withDominanceFunction(new DominanceFunctions.EarliestArrival())
      .withRequest(streetSearchRequest);

    if (reverse) {
      return builder.withTo(root).getShortestPathTree();
    }

    return builder.withFrom(root).getShortestPathTree();
  }

  /**
   * Bounds a tree that only has to reach some targets, each within its own reach: no state later
   * than what the targets still need is expanded, and a target that has been reached needs no more
   * than the time it was reached at. A later state cannot lead to an earlier arrival.
   */
  static final class TargetReach implements SkipEdgeStrategy<State, Edge> {

    private final Map<Vertex, Long> needed = new HashMap<>();
    private long bound;

    TargetReach(Map<Vertex, Duration> reach) {
      reach.forEach((target, duration) -> needed.put(target, duration.toSeconds()));
      bound = maxNeeded();
    }

    @Override
    public boolean shouldSkipEdge(State current, Edge edge) {
      long elapsed = current.getElapsedTimeSeconds();
      var stillNeeded = needed.get(current.getVertex());
      if (stillNeeded != null && elapsed < stillNeeded) {
        needed.put(current.getVertex(), elapsed);
        bound = maxNeeded();
      }
      return elapsed > bound;
    }

    private long maxNeeded() {
      return needed.values().stream().mapToLong(Long::longValue).max().orElse(0);
    }
  }
}
