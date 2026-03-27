package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.utils.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CarpoolRouter} implementation that lazily computes shortest-path trees (SPTs)
 * from/to registered vertices.
 * <p>
 * This is more efficient than individual A* searches when many routes share common
 * origin or destination vertices, as each SPT is computed at most once and reused for all
 * queries involving that vertex. Trees are only computed when first needed by a
 * {@link #route} call, so vertices that are never routed through never incur the cost
 * of tree expansion. Results are cached to avoid redundant tree lookups.
 * <p>
 * Vertices must be registered via {@link #addVertex} before routing.
 * The router first attempts to use a forward tree from the origin;
 * if unavailable, it falls back to a reverse tree to the destination.
 * <p>
 * This class is not thread-safe. Each instance should be used from a single thread.
 */
public class CarpoolTreeStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolTreeStreetRouter.class);

  private final Map<Vertex, VertexRegistration> forwardRegistrations = new HashMap<>();
  private final Map<Vertex, VertexRegistration> reverseRegistrations = new HashMap<>();
  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> forwardTrees = new HashMap<>();
  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> reverseTrees = new HashMap<>();
  private final Map<Pair<Vertex>, GraphPath<State, Edge, Vertex>> pathCache = new HashMap<>();
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
    /**
     *  We want to calculate paths both from and to the vertex
     */
    BOTH,
  }

  private record VertexRegistration(Vertex vertex, Duration searchLimit) {}

  private ShortestPathTree<State, Edge, Vertex> createTree(
    Vertex vertex,
    boolean reverse,
    Duration searchLimit
  ) {
    var streetSearchRequest = reverse
      ? StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(true).build()
      : StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    var builder = StreetSearchBuilder.of()
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(searchLimit))
      .withDominanceFunction(new DominanceFunctions.EarliestArrival())
      .withRequest(streetSearchRequest);

    if (reverse) {
      return builder.withTo(vertex).getShortestPathTree();
    }

    return builder.withFrom(vertex).getShortestPathTree();
  }

  private ShortestPathTree<State, Edge, Vertex> getOrCreateForwardTree(Vertex vertex) {
    var tree = forwardTrees.get(vertex);
    if (tree != null) {
      return tree;
    }
    var reg = forwardRegistrations.get(vertex);
    if (reg == null) {
      return null;
    }
    tree = createTree(vertex, false, reg.searchLimit());
    forwardTrees.put(vertex, tree);
    forwardRegistrations.remove(vertex);
    return tree;
  }

  private ShortestPathTree<State, Edge, Vertex> getOrCreateReverseTree(Vertex vertex) {
    var tree = reverseTrees.get(vertex);
    if (tree != null) {
      return tree;
    }
    var reg = reverseRegistrations.get(vertex);
    if (reg == null) {
      return null;
    }
    tree = createTree(vertex, true, reg.searchLimit());
    reverseTrees.put(vertex, tree);
    reverseRegistrations.remove(vertex);
    return tree;
  }

  /**
   * Register a vertex for tree computation in the given direction(s).
   * Tree computation is deferred until a {@link #route} call actually needs the tree.
   * Vertices whose trees are never needed incur no computation cost. Adding vertices after
   * routing has started is disallowed to ensure that all temporary vertices are linked to the
   * graph before any SPT is created. Otherwise, a previously computed tree may not contain
   * edges leading to the late-added vertex, making it unreachable.
   *
   * @param vertex     the street vertex to build trees from/to
   * @param direction  whether to build a forward tree ({@link Direction#FROM}),
   *                   a reverse tree ({@link Direction#TO}), or both ({@link Direction#BOTH})
   * @param searchLimit maximum duration for the tree expansion
   * @throws IllegalStateException if called after {@link #route} has already been invoked
   */
  public void addVertex(Vertex vertex, Direction direction, Duration searchLimit) {
    if (routingStarted) {
      throw new IllegalStateException(
        "Cannot add vertices after routing has started. " +
          "Register all vertices before calling route()."
      );
    }
    if (direction == Direction.FROM || direction == Direction.BOTH) {
      forwardRegistrations.put(vertex, new VertexRegistration(vertex, searchLimit));
    }
    if (direction == Direction.TO || direction == Direction.BOTH) {
      reverseRegistrations.put(vertex, new VertexRegistration(vertex, searchLimit));
    }
  }

  /** Returns the total number of forward vertices (pending and computed). Package-private for testing. */
  int forwardTreeCount() {
    return forwardRegistrations.size() + forwardTrees.size();
  }

  /** Returns the total number of reverse vertices (pending and computed). Package-private for testing. */
  int reverseTreeCount() {
    return reverseRegistrations.size() + reverseTrees.size();
  }

  /**
   * Find the shortest path between two vertices using lazily computed trees.
   * Results are cached so repeated queries for the same vertex pair are free.
   * The tree for a vertex is computed on the first {@link #route} call that needs it.
   * <p>
   * The method first looks for a forward tree rooted at {@code from}; if none exists
   * it falls back to a reverse tree rooted at {@code to}. Returns {@code null} if
   * neither tree is available or no path exists.
   */
  @Override
  public GraphPath<State, Edge, Vertex> route(Vertex from, Vertex to) {
    routingStarted = true;

    var key = new Pair<>(from, to);
    if (pathCache.containsKey(key)) {
      return pathCache.get(key);
    }

    var isReverse = false;
    var tree = getOrCreateForwardTree(from);
    if (tree == null) {
      tree = getOrCreateReverseTree(to);
      isReverse = true;
    }
    if (tree == null) {
      LOG.error("tree is null for vertices from {} to {}", from, to);
      return null;
    }

    var path = isReverse ? tree.getPath(from) : tree.getPath(to);
    pathCache.put(key, path);
    return path;
  }
}
