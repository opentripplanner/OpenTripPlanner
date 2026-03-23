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
 * A {@link CarpoolRouter} implementation that pre-computes shortest-path trees (SPTs)
 * from/to registered vertices.
 * <p>
 * This is more efficient than individual A* searches when many routes share common
 * origin or destination vertices, as each SPT is computed once and reused for all
 * queries involving that vertex. Results are cached to avoid redundant tree lookups.
 * <p>
 * Vertices must be registered via {@link #addVertex} before routing. The router first
 * attempts to use a forward tree from the origin; if unavailable, it falls back to a
 * reverse tree to the destination.
 */
public class CarpoolTreeStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolTreeStreetRouter.class);

  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> trees = new HashMap<>();
  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> reverseTrees = new HashMap<>();
  private final Map<Pair<Vertex>, GraphPath<State, Edge, Vertex>> pathCache = new HashMap<>();

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

  public void addVertex(Vertex vertex, Direction direction, Duration searchLimit) {
    if (
      !this.trees.containsKey(vertex) &&
      (direction == Direction.FROM || direction == Direction.BOTH)
    ) {
      this.trees.put(vertex, createTree(vertex, false, searchLimit));
    }

    if (
      !this.reverseTrees.containsKey(vertex) &&
      (direction == Direction.TO || direction == Direction.BOTH)
    ) {
      this.reverseTrees.put(vertex, createTree(vertex, true, searchLimit));
    }
  }

  /** Returns the number of forward trees. Package-private for testing. */
  int forwardTreeCount() {
    return trees.size();
  }

  /** Returns the number of reverse trees. Package-private for testing. */
  int reverseTreeCount() {
    return reverseTrees.size();
  }

  @Override
  public GraphPath<State, Edge, Vertex> route(Vertex from, Vertex to) {
    var key = new Pair<>(from, to);
    if (pathCache.containsKey(key)) {
      return pathCache.get(key);
    }

    var isReverse = false;
    var tree = this.trees.get(from);
    if (tree == null) {
      tree = this.reverseTrees.get(to);
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
