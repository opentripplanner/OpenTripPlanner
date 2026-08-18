package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/**
 * Resolves a car-reachable vertex near a target that sits on a pedestrian-only edge, or on a car
 * edge no car can actually reach.
 * <p>
 * The input is returned unchanged when already car-reachable; otherwise a bounded WALK A* finds
 * the cheapest car-reachable vertex within {@code maxWalk} and returns the walk path bridging the
 * gap, or {@code null} if none is reachable.
 * <p>
 * "Car-reachable" means a car can both reach the vertex and leave it, since a carpool pickup or
 * dropoff is a mid-route insertion the driver has to arrive at and continue from. Each direction is
 * checked in two stages: a cheap local pre-filter (a car-permitting street edge in that direction)
 * then a bounded {@link StreetMode#CAR} probe that a car can drive at least the minimum escape
 * distance in it (set per instance — see the constructor and {@link #createDefault()}). The probe
 * catches car edges that permit cars "on paper" but are stranded on a one-way stub, barrier pocket,
 * or disconnected island. Verdicts are cached per vertex.
 * <p>
 * {@link #snapPickup}/{@link #snapDropoff} walk from/to {@code vertexToSnap};
 * {@link #snapToPermanentVertex} accepts only permanent vertices.
 */
public final class CarReachableVertexSnapper {

  /**
   * Default escape distance: large enough to clear a connected vertex, small enough to reject a
   * stranded stub or island.
   */
  private static final double DEFAULT_MIN_CAR_ESCAPE_METERS = 500;

  private static final StreetSearchRequest CAR_DEPART = StreetSearchRequest.of()
    .withMode(StreetMode.CAR)
    .build();
  private static final StreetSearchRequest CAR_ARRIVE = StreetSearchRequest.copyOf(CAR_DEPART)
    .withArriveBy(true)
    .build();

  private final double minCarEscapeMeters;

  /**
   * Cached reachability verdicts per permanent vertex. Temporary vertices are never cached (they
   * never recur). Verdicts depend only on the static graph and escape distance, so they stay valid
   * across requests.
   */
  private final Map<Vertex, Boolean> carReachableCache = new ConcurrentHashMap<>();

  /**
   * @param minCarEscapeMeters straight-line distance, in metres, a car must be able to drive both
   *        to and from a candidate for it to be accepted; also caps the probe's exploration.
   */
  public CarReachableVertexSnapper(double minCarEscapeMeters) {
    this.minCarEscapeMeters = minCarEscapeMeters;
  }

  /** Creates a snapper with the default escape distance. */
  public static CarReachableVertexSnapper createDefault() {
    return new CarReachableVertexSnapper(DEFAULT_MIN_CAR_ESCAPE_METERS);
  }

  /**
   * A car-reachable {@code vertex} paired with the {@code walkPath} bridging the gap to the
   * original input, or a {@code null} {@code walkPath} when there is no real walking.
   */
  public record SnapResult(Vertex vertex, @Nullable GraphPath<State, Edge, Vertex> walkPath) {}

  /**
   * Snaps a pickup: forward search from {@code vertexToSnap}; the walk path runs to the snapped
   * vertex.
   */
  @Nullable
  public SnapResult snapPickup(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk
  ) {
    return snap(baseRequest, vertexToSnap, maxWalk, false, false);
  }

  /**
   * Snaps a dropoff: reverse search to {@code vertexToSnap}; the walk path runs from the snapped
   * vertex.
   */
  @Nullable
  public SnapResult snapDropoff(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk
  ) {
    return snap(baseRequest, vertexToSnap, maxWalk, true, false);
  }

  /**
   * Like {@link #snapPickup} but accepts only permanent vertices, so the result may outlive the
   * temporary linking. Permanent boundary vertices of the input's own linking are tried before any
   * walk search.
   */
  @Nullable
  public SnapResult snapToPermanentVertex(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk
  ) {
    return snap(baseRequest, vertexToSnap, maxWalk, false, true);
  }

  /**
   * @param baseRequest walk preferences for the walk A*; mode is forced to {@link StreetMode#WALK}
   *        and {@code arriveBy} overridden. The reachability probe ignores it (always plain
   *        {@link StreetMode#CAR}).
   * @param vertexToSnap the vertex to snap; returned unchanged when it can already be snapped to.
   * @param maxWalk walk budget for reaching a car-reachable vertex.
   * @param arriveBy {@code false} to walk forward from {@code vertexToSnap}, {@code true} to walk
   *        backward to it.
   * @param permanentOnly reject temporary vertices.
   * @return the snap result, or {@code null} if none is reachable within {@code maxWalk}.
   */
  @Nullable
  private SnapResult snap(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk,
    boolean arriveBy,
    boolean permanentOnly
  ) {
    // Confine traversal to this search's own linking, never a foreign one (see TraversalScope).
    var scope = TraversalScope.withOwnLinkingOf(vertexToSnap);

    if (canSnapTo(vertexToSnap, permanentOnly, scope)) {
      return new SnapResult(vertexToSnap, null);
    }

    if (permanentOnly) {
      for (var candidate : scope.permanentBoundary(vertexToSnap)) {
        if (isCarReachable(candidate)) {
          return new SnapResult(candidate, null);
        }
      }
    }

    // A* has no "first match" mode: the termination strategy is the predicate, invoked per state in
    // cost order, so the first accepted vertex is the cheapest. It returns only a boolean, so the
    // winning state is stashed in foundRef.
    State[] foundRef = new State[1];
    SearchTerminationStrategy<State> terminator = state -> {
      if (canSnapTo(state.getVertex(), permanentOnly, scope)) {
        foundRef[0] = state;
        return true;
      }
      return false;
    };

    var request = StreetSearchRequest.copyOf(baseRequest)
      .withMode(StreetMode.WALK)
      .withArriveBy(arriveBy)
      .build();
    // No heuristic is available since there is no fixed destination.
    var builder = StreetSearchBuilder.of()
      .withRequest(request)
      .withSkipEdgeStrategy(
        new ComposingSkipEdgeStrategy<>(scope, new DurationSkipEdgeStrategy<>(maxWalk))
      )
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withTerminationStrategy(terminator);
    // Reverse search starts from toVertices, forward from fromVertices.
    if (arriveBy) {
      builder = builder.withTo(vertexToSnap);
    } else {
      builder = builder.withFrom(vertexToSnap);
    }
    builder.run();

    State best = foundRef[0];
    if (best == null) {
      return null;
    }

    var path = new GraphPath<>(best);
    // A zero-duration path means the snap landed on a zero-cost temporary hop — no real walk.
    if (path.getDuration() == 0) {
      return new SnapResult(best.getVertex(), null);
    }
    return new SnapResult(best.getVertex(), path);
  }

  /**
   * Whether {@code vertex} may be returned as the snap result: car-reachable, and permanent when the
   * caller asked for {@code permanentOnly}.
   * <p>
   * A permanent vertex's verdict rests on the static graph alone, so it is cached and shared across
   * requests. A temporary vertex's rests on {@code scope}, which is specific to one linking, so it is
   * computed fresh and never stored.
   */
  private boolean canSnapTo(Vertex vertex, boolean permanentOnly, TraversalScope scope) {
    if (TraversalScope.isPermanent(vertex)) {
      return isCarReachable(vertex);
    }
    return !permanentOnly && computeCarReachable(vertex, scope);
  }

  /**
   * Whether {@code vertex} is car-reachable on the static street graph alone. Verdicts are cached,
   * since they cannot change while the graph stands.
   */
  public boolean isCarReachable(Vertex vertex) {
    Boolean cached = carReachableCache.get(vertex);
    if (cached != null) {
      return cached;
    }
    boolean verdict = computeCarReachable(vertex, TraversalScope.STATIC_GRAPH);
    carReachableCache.putIfAbsent(vertex, verdict);
    return verdict;
  }

  /**
   * Car-reachable when, in both directions, the local pre-filter (a car-permitting edge in that
   * direction) and the reachability probe pass. Both pre-filters run first, rejecting most vertices
   * without routing.
   */
  private boolean computeCarReachable(Vertex vertex, SkipEdgeStrategy<State, Edge> skipEdges) {
    return (
      anyStreetEdgeAllowsCar(vertex.getOutgoing()) &&
      anyStreetEdgeAllowsCar(vertex.getIncoming()) &&
      probeEscapes(vertex, false, skipEdges) &&
      probeEscapes(vertex, true, skipEdges)
    );
  }

  /**
   * Bounded CAR search reporting whether any settled vertex lies at least
   * {@link #minCarEscapeMeters} away — outward when {@code arriveBy} is {@code false}, inward when
   * {@code true}. Terminates as soon as one is far enough; a stranded vertex only exhausts its small
   * pocket.
   */
  private boolean probeEscapes(
    Vertex origin,
    boolean arriveBy,
    SkipEdgeStrategy<State, Edge> skipEdges
  ) {
    Coordinate originCoordinate = origin.getCoordinate();
    boolean[] escaped = new boolean[1];
    SearchTerminationStrategy<State> terminator = state -> {
      if (
        SphericalDistanceLibrary.fastDistance(
          originCoordinate,
          state.getVertex().getCoordinate()
        ) >=
        minCarEscapeMeters
      ) {
        escaped[0] = true;
        return true;
      }
      return false;
    };

    // The Euclidean heuristic is not appropriate without a fixed destination, and a purpose-built
    // one is out of scope for now, so this runs as plain Dijkstra (f = g).
    var builder = StreetSearchBuilder.of()
      .withRequest(arriveBy ? CAR_ARRIVE : CAR_DEPART)
      .withSkipEdgeStrategy(skipEdges)
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withTerminationStrategy(terminator);
    if (arriveBy) {
      builder = builder.withTo(origin);
    } else {
      builder = builder.withFrom(origin);
    }
    builder.run();

    return escaped[0];
  }

  private static boolean anyStreetEdgeAllowsCar(Iterable<Edge> edges) {
    for (Edge e : edges) {
      if (e instanceof StreetEdge se && se.getPermission().allows(TraverseMode.CAR)) {
        return true;
      }
    }
    return false;
  }
}
