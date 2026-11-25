package org.opentripplanner.ext.carpooling.routing;

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
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.utils.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CarpoolRouter} implementation that lazily computes shortest-path trees (SPTs) from/to
 * registered vertices.
 * <p>
 * This is more efficient than individual A* searches when many routes share common origin or
 * destination vertices, as each SPT is computed at most once and reused for all queries involving
 * that vertex. Trees are only computed when first needed by a {@link #route} call, so vertices that
 * are never routed through never incur the cost of tree expansion. Results are cached to avoid
 * redundant tree lookups.
 * <p>
 * Vertices must be registered via {@link #addVertex} before routing. The router first attempts
 * to use a forward tree from the origin; if unavailable, it falls back to a reverse tree to the
 * destination.
 * <p>
 * This class is not thread-safe. Each instance should be used from a single thread.
 */
public class CarpoolTreeStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolTreeStreetRouter.class);

  private final Trees forwardTrees = new Trees(false);
  private final Trees reverseTrees = new Trees(true);
  private final Map<Pair<Vertex>, RoutedSegment> segmentCache = new HashMap<>();
  private boolean routingStarted = false;

  public enum Direction {
    /**
     * We want to calculate paths from the vertex
     */
    FROM,
    /**
     * We want to calculate paths to the vertex
     */
    TO,
  }

  /**
   * Register a vertex for tree computation in the given direction(s). Tree computation is deferred
   * until a {@link #route} call actually needs the tree. Vertices whose trees are never needed
   * incur no computation cost. Adding vertices after routing has started is disallowed to ensure
   * that all temporary vertices are linked to the graph before any SPT is created. Otherwise, a
   * previously computed tree may not contain edges leading to the late-added vertex, making it
   * unreachable.
   *
   * @param vertex      the street vertex to build trees from/to
   * @param direction   whether to build a forward tree ({@link Direction#FROM}) or a reverse tree
   *                    ({@link Direction#TO})
   * @param searchLimit maximum duration for the tree expansion
   * @throws IllegalStateException if called after {@link #route} has already been invoked
   */
  public void addVertex(Vertex vertex, Direction direction, Duration searchLimit) {
    addVertex(vertex, direction, searchLimit, null);
  }

  /**
   * Like {@link #addVertex(Vertex, Direction, Duration)}, for a tree that only has to reach the
   * given target vertices, each within its own reach: it stops expanding once no target can be
   * reached earlier than it already has.
   */
  public void addVertex(
    Vertex vertex,
    Direction direction,
    Duration searchLimit,
    @Nullable Map<Vertex, Duration> targets
  ) {
    if (routingStarted) {
      throw new IllegalStateException(
        "Cannot add vertices after routing has started. " +
          "Register all vertices before calling route()."
      );
    }
    var trees = direction == Direction.FROM ? forwardTrees : reverseTrees;
    trees.register(vertex, searchLimit, targets);
  }

  /**
   * Find the shortest path between two vertices using lazily computed trees. Results are cached so
   * repeated queries for the same vertex pair are free. The tree for a vertex is computed on the
   * first {@link #route} call that needs it.
   * <p>
   * The method first looks for a forward tree rooted at {@code from}; if none exists it falls
   * back to a reverse tree rooted at {@code to}. At least one of the two endpoints must therefore
   * have been registered with {@link #addVertex} in the matching direction; a call whose endpoints
   * were both left unregistered cannot be served and returns {@code null}. A registered endpoint
   * whose tree does not reach the other endpoint within its search limit returns {@code null} as
   * well.
   */
  @Override
  public RoutedSegment route(Vertex from, Vertex to) {
    routingStarted = true;

    var key = new Pair<>(from, to);
    if (segmentCache.containsKey(key)) {
      return segmentCache.get(key);
    }

    var isReverse = false;
    var tree = forwardTrees.get(from);
    if (tree == null) {
      tree = reverseTrees.get(to);
      isReverse = true;
    }
    if (tree == null) {
      LOG.error("tree is null for vertices from {} to {}", from, to);
      return null;
    }

    var arrival = tree.getState(isReverse ? from : to);
    var segment = arrival == null ? null : new TreeSegment(from, to, arrival);
    segmentCache.put(key, segment);
    return segment;
  }

  /**
   * Returns the total number of forward vertices (pending and computed). Package-private for
   * testing.
   */
  int forwardTreeCount() {
    return forwardTrees.count();
  }

  /**
   * Returns the total number of reverse vertices (pending and computed). Package-private for
   * testing.
   */
  int reverseTreeCount() {
    return reverseTrees.count();
  }

  /** The trees of one direction: registered first, each built when it is first asked for. */
  private static final class Trees {

    private final boolean reverse;
    private final Map<Vertex, Registration> registrations = new HashMap<>();
    private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> trees = new HashMap<>();

    Trees(boolean reverse) {
      this.reverse = reverse;
    }

    /**
     * Registers {@code root} keeping the larger of any already-registered limit and
     * {@code searchLimit}: a vertex registered twice gets one tree, which has to serve both
     * registrations. A larger tree only widens the search, so taking the maximum is always safe.
     */
    void register(Vertex root, Duration searchLimit, @Nullable Map<Vertex, Duration> targets) {
      // A tree shared by two registrations must serve both, so it stops at neither one's targets.
      registrations.merge(root, new Registration(searchLimit, targets), (a, b) ->
        new Registration(max(a.searchLimit(), b.searchLimit()), null)
      );
    }

    /** The tree of {@code root}, or {@code null} if the vertex was not registered. */
    @Nullable
    ShortestPathTree<State, Edge, Vertex> get(Vertex root) {
      var tree = trees.get(root);
      if (tree != null) {
        return tree;
      }
      var registration = registrations.get(root);
      if (registration == null) {
        return null;
      }
      var targets = registration.targets();
      var targetReach = targets == null ? null : new TargetReach(targets);
      tree = carTree(root, reverse, registration.searchLimit(), targetReach);
      trees.put(root, tree);
      registrations.remove(root);
      return tree;
    }

    int count() {
      return registrations.size() + trees.size();
    }
  }

  private record Registration(Duration searchLimit, @Nullable Map<Vertex, Duration> targets) {}

  /**
   * A car shortest-path tree from {@code root}, or to it when {@code reverse}. Nothing is expanded
   * past {@code searchLimit}, nor where {@code alsoSkip}, if given, skips.
   */
  static ShortestPathTree<State, Edge, Vertex> carTree(
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

  private static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
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

  /**
   * A segment answered from a shortest-path tree. {@code arrival} is the tree's state at the end of
   * the segment that is not the tree's root: the segment's {@code to} for a forward tree, its
   * {@code from} for a reverse tree. The state's elapsed time is the segment's duration; walking
   * its back pointers yields the path, which {@link StreetPath} puts in chronological order for
   * both search directions.
   */
  static final class TreeSegment implements RoutedSegment {

    private final Vertex from;
    private final Vertex to;
    private final State arrival;

    @Nullable
    private StreetPath path;

    TreeSegment(Vertex from, Vertex to, State arrival) {
      this.from = from;
      this.to = to;
      this.arrival = arrival;
    }

    @Override
    public int durationSeconds() {
      return (int) arrival.getElapsedTimeSeconds();
    }

    @Override
    public StreetPath path() {
      if (path == null) {
        path = new StreetPath(arrival);
      }
      return path;
    }

    @Override
    public String toString() {
      return "TreeSegment{" + from + " -> " + to + ", " + durationSeconds() + "s}";
    }
  }
}
