package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * A {@link CarpoolRouter} that answers from one-to-many car trees rooted at registered vertices.
 * <p>
 * A tree is built ({@link CompactCarTree}) on the first {@link #route} call that needs it and then
 * serves every query from or to its root, so vertices that are never routed through cost nothing.
 * The router first looks for a forward tree rooted at {@code from}; if there is none it uses a
 * reverse tree rooted at {@code to}.
 * <p>
 * A segment's duration is read straight off the tree; its path is only assembled if
 * {@link RoutedSegment#path()} is called, which happens for the few segments that end up in an
 * itinerary.
 * <p>
 * Not thread-safe; one instance per request.
 */
public class CarpoolTreeStreetRouter implements CarpoolRouter {

  private final Map<Vertex, Duration> forwardLimits = new HashMap<>();
  private final Map<Vertex, Duration> reverseLimits = new HashMap<>();
  private final Map<Vertex, CompactCarTree> forwardTrees = new HashMap<>();
  private final Map<Vertex, CompactCarTree> reverseTrees = new HashMap<>();
  private boolean routingStarted = false;

  public enum Direction {
    /** Paths from the vertex. */
    FROM,
    /** Paths to the vertex. */
    TO,
    /** Both. */
    BOTH,
  }

  /**
   * Registers a vertex whose tree explores everything within {@code searchLimit}. Registering the
   * same vertex again keeps the larger limit: distinct temporary vertices at one coordinate compare
   * equal, so co-located waypoints share a tree, which must span the longest leg among them.
   * Vertices must be registered before routing starts, so that every temporary vertex is linked to
   * the graph before a tree is built.
   *
   * @throws IllegalStateException if called after {@link #route}
   */
  public void addVertex(Vertex vertex, Direction direction, Duration searchLimit) {
    if (routingStarted) {
      throw new IllegalStateException("Register all vertices before calling route()");
    }
    if (direction != Direction.TO) {
      forwardLimits.merge(vertex, searchLimit, CarpoolTreeStreetRouter::max);
    }
    if (direction != Direction.FROM) {
      reverseLimits.merge(vertex, searchLimit, CarpoolTreeStreetRouter::max);
    }
  }

  /**
   * The shortest car segment between two vertices, or {@code null} when neither end has a tree in
   * the needed direction or the tree does not reach the other end within its limit.
   */
  @Override
  @Nullable
  public RoutedSegment route(Vertex from, Vertex to) {
    routingStarted = true;
    var tree = tree(from, false);
    if (tree != null) {
      return segment(tree, from, to, to);
    }
    tree = tree(to, true);
    return tree == null ? null : segment(tree, from, to, from);
  }

  @Nullable
  private CompactCarTree tree(Vertex root, boolean reverse) {
    var trees = reverse ? reverseTrees : forwardTrees;
    var tree = trees.get(root);
    if (tree == null) {
      var limit = (reverse ? reverseLimits : forwardLimits).get(root);
      if (limit == null) {
        return null;
      }
      tree = CompactCarTree.build(root, reverse, limit);
      trees.put(root, tree);
    }
    return tree;
  }

  @Nullable
  private static RoutedSegment segment(CompactCarTree tree, Vertex from, Vertex to, Vertex farEnd) {
    int seconds = tree.elapsedSeconds(farEnd);
    return seconds < 0 ? null : new TreeSegment(from, to, seconds, tree, farEnd);
  }

  /** Number of registered forward vertices. Package-private for testing. */
  int forwardTreeCount() {
    return forwardLimits.size();
  }

  /** Number of registered reverse vertices. Package-private for testing. */
  int reverseTreeCount() {
    return reverseLimits.size();
  }

  private static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  /**
   * A segment answered from a tree. {@code farEnd} is the end that is not the tree's root. The
   * path is built on demand from the tree.
   */
  static final class TreeSegment implements RoutedSegment {

    private final Vertex from;
    private final Vertex to;
    private final int durationSeconds;
    private final CompactCarTree tree;
    private final Vertex farEnd;

    @Nullable
    private GraphPath<State, Edge, Vertex> path;

    TreeSegment(Vertex from, Vertex to, int durationSeconds, CompactCarTree tree, Vertex farEnd) {
      this.from = from;
      this.to = to;
      this.durationSeconds = durationSeconds;
      this.tree = tree;
      this.farEnd = farEnd;
    }

    @Override
    public Vertex from() {
      return from;
    }

    @Override
    public Vertex to() {
      return to;
    }

    @Override
    public int durationSeconds() {
      return durationSeconds;
    }

    @Override
    public GraphPath<State, Edge, Vertex> path() {
      if (path == null) {
        path = tree.path(farEnd);
      }
      return path;
    }

    @Override
    public String toString() {
      return "TreeSegment{" + from + " -> " + to + ", " + durationSeconds + "s}";
    }
  }
}
