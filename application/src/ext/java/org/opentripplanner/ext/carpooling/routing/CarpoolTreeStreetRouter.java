package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.StreetConstants;
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
 * A {@link #route} answer is read straight off the tree: the arrival state at the far end gives
 * the segment's duration, and the full path is only assembled from the state's back pointers if
 * {@link RoutedSegment#path()} is called — which happens for the few segments that make it into
 * an itinerary, not for the thousands evaluated and discarded per request.
 * <p>
 * Vertices must be registered via {@link #addVertex} or {@link #addLeg} before routing.
 * The router first attempts to use a forward tree from the origin;
 * if unavailable, it falls back to a reverse tree to the destination.
 * <p>
 * A tree registered with {@link #addVertex} explores everything within its duration limit — a
 * disc around the root. A tree registered with {@link #addLeg} is bounded to the ellipse in which
 * a detour of the leg can still be feasible, see {@link EllipseBoundSkipEdgeStrategy}; for long
 * legs that is a small fraction of the disc.
 * <p>
 * This class is not thread-safe. Each instance should be used from a single thread.
 */
public class CarpoolTreeStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolTreeStreetRouter.class);

  private final double maxCarSpeedMetersPerSecond;
  private final Map<Vertex, VertexRegistration> forwardRegistrations = new HashMap<>();
  private final Map<Vertex, VertexRegistration> reverseRegistrations = new HashMap<>();
  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> forwardTrees = new HashMap<>();
  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> reverseTrees = new HashMap<>();
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
    /**
     *  We want to calculate paths both from and to the vertex
     */
    BOTH,
  }

  /**
   * Uses {@link StreetConstants#DEFAULT_MAX_CAR_SPEED} to bound leg trees. Prefer the constructor
   * taking the graph's real maximum car speed, which prunes tighter while staying safe.
   */
  public CarpoolTreeStreetRouter() {
    this(StreetConstants.DEFAULT_MAX_CAR_SPEED);
  }

  /**
   * @param maxCarSpeedMetersPerSecond the fastest speed any street in the graph can be driven at,
   *        used to bound leg trees to their feasibility ellipse — see {@link #addLeg}.
   */
  public CarpoolTreeStreetRouter(double maxCarSpeedMetersPerSecond) {
    if (maxCarSpeedMetersPerSecond <= 0) {
      throw new IllegalArgumentException("maxCarSpeed must be positive");
    }
    this.maxCarSpeedMetersPerSecond = maxCarSpeedMetersPerSecond;
  }

  /**
   * What a tree rooted at {@code vertex} has to cover. {@code foci} lists the far ends of the legs
   * the tree serves, each with the most seconds that leg may take; the tree is then bounded to the
   * union of the corresponding ellipses. A {@code null} {@code foci} means a plain disc of radius
   * {@code searchLimit} — everything within the limit is wanted.
   */
  private record VertexRegistration(
    Vertex vertex,
    Duration searchLimit,
    @Nullable List<EllipseBoundSkipEdgeStrategy.Focus> foci
  ) {
    /**
     * Combines two registrations of the same vertex and direction: the larger limit wins, and the
     * ellipses are united. A disc absorbs any ellipse — everything a disc registration asks for
     * must stay reachable.
     */
    VertexRegistration merge(VertexRegistration other) {
      var limit = searchLimit.compareTo(other.searchLimit) >= 0 ? searchLimit : other.searchLimit;
      if (foci == null || other.foci == null) {
        return new VertexRegistration(vertex, limit, null);
      }
      var union = new ArrayList<EllipseBoundSkipEdgeStrategy.Focus>(
        foci.size() + other.foci.size()
      );
      union.addAll(foci);
      union.addAll(other.foci);
      return new VertexRegistration(vertex, limit, union);
    }
  }

  private ShortestPathTree<State, Edge, Vertex> createTree(
    Vertex vertex,
    boolean reverse,
    VertexRegistration registration
  ) {
    var streetSearchRequest = reverse
      ? StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(true).build()
      : StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    var builder = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withSkipEdgeStrategy(skipEdgeStrategy(registration, reverse))
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
    tree = createTree(vertex, false, reg);
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
    tree = createTree(vertex, true, reg);
    reverseTrees.put(vertex, tree);
    reverseRegistrations.remove(vertex);
    return tree;
  }

  /**
   * The search bound of a tree: never beyond the registered duration limit, and — for a tree that
   * only serves legs — never outside the union of the legs' feasibility ellipses.
   */
  private SkipEdgeStrategy<State, Edge> skipEdgeStrategy(
    VertexRegistration registration,
    boolean reverse
  ) {
    SkipEdgeStrategy<State, Edge> byDuration = new DurationSkipEdgeStrategy<>(
      registration.searchLimit()
    );
    if (registration.foci() == null) {
      return byDuration;
    }
    return new ComposingSkipEdgeStrategy<>(
      byDuration,
      new EllipseBoundSkipEdgeStrategy(registration.foci(), maxCarSpeedMetersPerSecond, reverse)
    );
  }

  /**
   * Register a vertex for tree computation in the given direction(s). The tree explores everything
   * within {@code searchLimit} of the vertex. Use {@link #addLeg} instead when the tree only has to
   * serve detours of a known leg.
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
      register(forwardRegistrations, new VertexRegistration(vertex, searchLimit, null));
    }
    if (direction == Direction.TO || direction == Direction.BOTH) {
      register(reverseRegistrations, new VertexRegistration(vertex, searchLimit, null));
    }
  }

  /**
   * Register the two trees a driver leg needs — a forward tree from {@code from} and a reverse tree
   * to {@code to} — bounded to the region where a detour of the leg can still be feasible.
   * <p>
   * {@code legLimit} is the most the leg may take once a passenger is inserted: its baseline
   * duration plus the allowed deviation (and any slack). Every segment the trees are asked for —
   * {@code from → pickup}, {@code from → stop}, {@code pickup → to}, {@code stop → to}, and the
   * baseline itself — is part of a detour that starts at {@code from}, ends at {@code to} and takes
   * at most {@code legLimit}, so no vertex outside the ellipse with those foci and that bound is
   * ever needed. The plain duration limit is kept as well: the ellipse never reaches beyond it.
   * <p>
   * Registering the same vertex and direction again — for another leg, or via {@link #addVertex} —
   * widens the tree: the limits are maxed and the ellipses united, and a disc registration turns
   * the tree back into a plain disc.
   *
   * @throws IllegalStateException if called after {@link #route} has already been invoked, see
   *         {@link #addVertex}
   */
  public void addLeg(Vertex from, Vertex to, Duration legLimit) {
    if (routingStarted) {
      throw new IllegalStateException(
        "Cannot add legs after routing has started. Register all legs before calling route()."
      );
    }
    long bound = legLimit.toSeconds();
    register(
      forwardRegistrations,
      new VertexRegistration(
        from,
        legLimit,
        List.of(new EllipseBoundSkipEdgeStrategy.Focus(to.getCoordinate(), bound))
      )
    );
    register(
      reverseRegistrations,
      new VertexRegistration(
        to,
        legLimit,
        List.of(new EllipseBoundSkipEdgeStrategy.Focus(from.getCoordinate(), bound))
      )
    );
  }

  /**
   * Registers a tree, widening any registration already present for the same vertex — see
   * {@link VertexRegistration#merge}. Distinct
   * {@link org.opentripplanner.street.model.vertex.TemporaryStreetLocation}s at the same
   * coordinate compare equal, so two trips — or two legs of one trip — routing through the same
   * point collapse to a single registration here. The shared tree must span the longest leg
   * registered at that point, so the largest limit wins: a smaller limit would build a tree too
   * short for a longer leg's baseline, making it unroutable. An over-large tree only widens the
   * search — any insertion it would wrongly admit is rejected by the delay constraints — so
   * widening is always safe.
   */
  private static void register(
    Map<Vertex, VertexRegistration> registrations,
    VertexRegistration registration
  ) {
    registrations.merge(registration.vertex(), registration, VertexRegistration::merge);
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
   * The method first looks for a forward tree rooted at {@code from}; if none exists it falls back
   * to a reverse tree rooted at {@code to}. At least one of the two endpoints must therefore have
   * been registered with {@link #addVertex} in the matching direction; a call whose endpoints were
   * both left unregistered cannot be served and returns {@code null}. A registered endpoint whose
   * tree does not reach the other endpoint within its search limit returns {@code null} as well.
   * <p>
   * The returned segment holds the tree's arrival state at the far end. Its duration is that
   * state's elapsed time; its path is only assembled when {@link RoutedSegment#path()} is called.
   */
  @Override
  public RoutedSegment route(Vertex from, Vertex to) {
    routingStarted = true;

    var key = new Pair<>(from, to);
    if (segmentCache.containsKey(key)) {
      return segmentCache.get(key);
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

    var arrival = tree.getState(isReverse ? from : to);
    var segment = arrival == null ? null : new TreeSegment(from, to, arrival);
    segmentCache.put(key, segment);
    return segment;
  }

  /**
   * A segment answered from a shortest-path tree. {@code arrival} is the tree's state at the end
   * of the segment that is not the tree's root: the segment's {@code to} for a forward tree, its
   * {@code from} for a reverse tree. The state's elapsed time is the segment's duration; walking
   * its back pointers yields the path, which {@link GraphPath} puts in chronological order for
   * both search directions.
   */
  static final class TreeSegment implements RoutedSegment {

    private final Vertex from;
    private final Vertex to;
    private final State arrival;

    @Nullable
    private GraphPath<State, Edge, Vertex> path;

    TreeSegment(Vertex from, Vertex to, State arrival) {
      this.from = from;
      this.to = to;
      this.arrival = arrival;
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
      return (int) arrival.getElapsedTimeSeconds();
    }

    @Override
    public GraphPath<State, Edge, Vertex> path() {
      if (path == null) {
        path = new GraphPath<>(arrival);
      }
      return path;
    }

    /** Whether {@link #path()} has been called. Package-private for testing. */
    boolean isPathMaterialized() {
      return path != null;
    }

    @Override
    public String toString() {
      return "TreeSegment{" + from + " -> " + to + ", " + durationSeconds() + "s}";
    }
  }
}
