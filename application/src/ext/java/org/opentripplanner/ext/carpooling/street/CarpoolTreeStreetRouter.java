package org.opentripplanner.ext.carpooling.street;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.ext.carpooling.street.CarTreeSearch.TargetReach;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CarpoolRouter} that answers from shortest-path trees rooted at registered vertices,
 * which is cheaper than an A* search per segment when many segments share an end. A tree is built
 * the first time a {@link #route} call needs it, and every answer is kept.
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
   * Registers a vertex for a tree from it ({@link Direction#FROM}) or to it ({@link Direction#TO}),
   * expanded no further than {@code searchLimit}.
   *
   * @throws IllegalStateException if called after {@link #route}: a tree built earlier would lack
   *                               the temporary edges linking a vertex added later
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
   * The segment from the forward tree of {@code from} or, if that vertex has none, from the reverse
   * tree of {@code to}. {@code null} if neither vertex is registered in that direction, or if the
   * tree does not reach the other vertex within its search limit.
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

  /** The number of forward trees, built or not. Package-private for testing. */
  int forwardTreeCount() {
    return forwardTrees.count();
  }

  /** The number of reverse trees, built or not. Package-private for testing. */
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
      tree = CarTreeSearch.carTree(root, reverse, registration.searchLimit(), targetReach);
      trees.put(root, tree);
      registrations.remove(root);
      return tree;
    }

    int count() {
      return registrations.size() + trees.size();
    }
  }

  private record Registration(Duration searchLimit, @Nullable Map<Vertex, Duration> targets) {}

  private static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
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
