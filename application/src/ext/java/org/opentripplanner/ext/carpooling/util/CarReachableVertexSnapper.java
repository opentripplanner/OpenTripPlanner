package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
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
 * "Car-reachable" is checked for a {@link CarReachabilityDirection} in two stages: a cheap local
 * pre-filter (a car-permitting street edge in the required direction) then a bounded
 * {@link StreetMode#CAR} probe that a car can drive at least {@link #DEFAULT_MIN_CAR_ESCAPE_METERS}
 * to or from the vertex. The probe catches car edges that permit cars "on paper" but are stranded
 * on a one-way stub, barrier pocket, or disconnected island. Verdicts are cached per vertex and
 * direction.
 * <p>
 * {@link #snapPickup}/{@link #snapDropoff} walk from/to {@code vertexToSnap};
 * {@link #snapToPermanentVertex} accepts only permanent vertices.
 */
public final class CarReachableVertexSnapper {

  /**
   * A candidate is accepted only if a car can drive at least this far (straight-line, metres) to or
   * from it: large enough to clear a connected vertex, small enough to reject a stranded stub or
   * island. Also caps the probe's exploration.
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
   * Cached reachability verdicts per permanent vertex — whether a car can depart it
   * ({@code canDepartCache}) or reach it ({@code canArriveCache}), cached separately because in
   * some circumstances they may differ. Temporary vertices are never cached (they never recur).
   * Verdicts depend only on the static graph and escape distance, so they stay valid across
   * requests.
   */
  private final Map<Vertex, Boolean> canDepartCache = new ConcurrentHashMap<>();
  private final Map<Vertex, Boolean> canArriveCache = new ConcurrentHashMap<>();

  /**
   * @param minCarEscapeMeters distance a car must be able to drive to/from a candidate for it to be
   *        accepted; also caps the probe's exploration.
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
    return snap(baseRequest, vertexToSnap, maxWalk, false, false, CarReachabilityDirection.THROUGH);
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
    return snap(baseRequest, vertexToSnap, maxWalk, true, false, CarReachabilityDirection.THROUGH);
  }

  /**
   * Like {@link #snapPickup} but accepts only permanent vertices, so the result may outlive the
   * temporary linking. Pass {@link CarReachabilityDirection#DEPART} for a departure-only endpoint
   * and {@link CarReachabilityDirection#ARRIVE} for an arrival-only one; the walk search runs
   * backward for {@code ARRIVE}, forward otherwise. Permanent boundary vertices of the input's own
   * linking are tried before any walk search.
   */
  @Nullable
  public SnapResult snapToPermanentVertex(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk,
    CarReachabilityDirection reachability
  ) {
    boolean arriveBy = reachability == CarReachabilityDirection.ARRIVE;
    return snap(baseRequest, vertexToSnap, maxWalk, arriveBy, true, reachability);
  }

  /**
   * @param baseRequest walk preferences for the walk A*; mode is forced to {@link StreetMode#WALK}
   *        and {@code arriveBy} overridden. The reachability probe ignores it (always plain
   *        {@link StreetMode#CAR}).
   * @param vertexToSnap the vertex to snap; returned unchanged when already acceptable.
   * @param maxWalk walk budget for reaching a car-reachable vertex.
   * @param arriveBy {@code false} for a pickup, {@code true} for a dropoff.
   * @param permanentOnly reject temporary vertices.
   * @param reachability car-travel direction(s) the snapped vertex must support; independent of
   *        {@code arriveBy}.
   * @return the snap result, or {@code null} if none is reachable within {@code maxWalk}.
   */
  @Nullable
  private SnapResult snap(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk,
    boolean arriveBy,
    boolean permanentOnly,
    CarReachabilityDirection reachability
  ) {
    // Confine temporary-edge traversal to this search's own linking (see isForeignTempEdge).
    var ownTempVertices = ownLinking(vertexToSnap);

    if (isAcceptableTarget(vertexToSnap, permanentOnly, reachability, ownTempVertices)) {
      return new SnapResult(vertexToSnap, null);
    }

    if (permanentOnly) {
      for (var candidate : permanentBoundary(vertexToSnap, ownTempVertices)) {
        if (isCarReachable(candidate, reachability)) {
          return new SnapResult(candidate, null);
        }
      }
    }

    // A* has no "first match" mode: the termination strategy is the predicate, invoked per state in
    // cost order, so the first accepted vertex is the cheapest. It returns only a boolean, so the
    // winning state is stashed in foundRef.
    State[] foundRef = new State[1];
    SearchTerminationStrategy<State> terminator = state -> {
      if (isAcceptableTarget(state.getVertex(), permanentOnly, reachability, ownTempVertices)) {
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
        new ComposingSkipEdgeStrategy<>(
          new ForeignTempEdgeSkipStrategy(ownTempVertices),
          new DurationSkipEdgeStrategy<>(maxWalk)
        )
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

  private boolean isAcceptableTarget(
    Vertex vertex,
    boolean permanentOnly,
    CarReachabilityDirection reachability,
    Set<Vertex> ownTempVertices
  ) {
    if (permanentOnly && vertex instanceof TemporaryVertex) {
      return false;
    }
    return isCarReachable(vertex, reachability, ownTempVertices);
  }

  /**
   * Whether {@code vertex} is car-reachable for {@code reachability} on the static street graph
   * alone (temporary edges ignored).
   */
  public boolean isCarReachable(Vertex vertex, CarReachabilityDirection reachability) {
    return isCarReachable(vertex, reachability, Set.of());
  }

  /**
   * Car-reachable when the local pre-filter (a car-permitting edge in the required direction) and
   * the reachability probe both pass. The pre-filter runs first, rejecting most vertices without
   * routing.
   *
   * @param ownTempVertices temporary vertices the probe may cross; empty for a static-graph check.
   */
  private boolean isCarReachable(
    Vertex vertex,
    CarReachabilityDirection reachability,
    Set<Vertex> ownTempVertices
  ) {
    if (reachability.requiresDeparture() && !anyStreetEdgeAllowsCar(vertex.getOutgoing())) {
      return false;
    }
    if (reachability.requiresArrival() && !anyStreetEdgeAllowsCar(vertex.getIncoming())) {
      return false;
    }
    if (reachability.requiresDeparture() && !canEscape(vertex, false, ownTempVertices)) {
      return false;
    }
    if (reachability.requiresArrival() && !canEscape(vertex, true, ownTempVertices)) {
      return false;
    }
    return true;
  }

  /**
   * Whether a car can escape {@code vertex} — leave it when {@code arriveBy} is {@code false}, reach
   * it when {@code true}. Permanent-vertex verdicts are cached; temporary vertices are probed
   * uncached and may cross their own linking's temporary edges, never a foreign one.
   */
  private boolean canEscape(Vertex vertex, boolean arriveBy, Set<Vertex> ownTempVertices) {
    if (vertex instanceof TemporaryVertex) {
      return probeEscapes(vertex, arriveBy, ownTempVertices);
    }
    var cache = arriveBy ? canArriveCache : canDepartCache;
    Boolean cached = cache.get(vertex);
    if (cached != null) {
      return cached;
    }
    // Probe is too slow to hold a bin lock (as computeIfAbsent would); a duplicate probe is harmless.
    boolean verdict = probeEscapes(vertex, arriveBy, Set.of());
    cache.putIfAbsent(vertex, verdict);
    return verdict;
  }

  /**
   * Bounded CAR search reporting whether any settled vertex lies at least
   * {@link #minCarEscapeMeters} away — outward when {@code arriveBy} is {@code false}, inward when
   * {@code true}. Terminates as soon as one is far enough; a stranded vertex only exhausts its small
   * pocket.
   */
  private boolean probeEscapes(Vertex origin, boolean arriveBy, Set<Vertex> ownTempVertices) {
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

    var builder = StreetSearchBuilder.of()
      .withRequest(arriveBy ? CAR_ARRIVE : CAR_DEPART)
      .withSkipEdgeStrategy(new ForeignTempEdgeSkipStrategy(ownTempVertices))
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

  /**
   * The temporary vertices of {@code start}'s own linking — every temporary vertex reachable without
   * crossing a permanent one. Empty when {@code start} is permanent. Foreign linkings attach only to
   * the permanent graph, so this never crosses into another request's subgraph.
   */
  private static Set<Vertex> ownLinking(Vertex start) {
    if (!(start instanceof TemporaryVertex)) {
      return Set.of();
    }
    var own = new HashSet<Vertex>();
    var queue = new ArrayDeque<Vertex>();
    own.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      var current = queue.poll();
      for (var edges : List.of(current.getOutgoing(), current.getIncoming())) {
        for (Edge edge : edges) {
          for (var neighbor : List.of(edge.getFromVertex(), edge.getToVertex())) {
            if (neighbor instanceof TemporaryVertex && own.add(neighbor)) {
              queue.add(neighbor);
            }
          }
        }
      }
    }
    return own;
  }

  /**
   * A temporary edge is foreign when neither endpoint is in {@code ownTempVertices}.
   */
  private static boolean isForeignTempEdge(Edge edge, Set<Vertex> ownTempVertices) {
    return (
      edge instanceof TemporaryEdge &&
      !ownTempVertices.contains(edge.getFromVertex()) &&
      !ownTempVertices.contains(edge.getToVertex())
    );
  }

  /** Skips foreign temporary edges (see {@link #isForeignTempEdge}). */
  private record ForeignTempEdgeSkipStrategy(Set<Vertex> ownTempVertices) implements
    SkipEdgeStrategy<State, Edge> {
    @Override
    public boolean shouldSkipEdge(State current, Edge edge) {
      return isForeignTempEdge(edge, ownTempVertices);
    }
  }

  /**
   * The permanent vertices bordering the input's own linking (split-edge endpoints or
   * directly-linked graph vertices), ordered by distance from {@code origin}.
   */
  private static List<Vertex> permanentBoundary(Vertex origin, Set<Vertex> ownTempVertices) {
    var seen = new HashSet<>(ownTempVertices);
    var boundary = new ArrayList<Vertex>();
    for (var temp : ownTempVertices) {
      for (var edges : List.of(temp.getOutgoing(), temp.getIncoming())) {
        for (Edge edge : edges) {
          for (var neighbor : List.of(edge.getFromVertex(), edge.getToVertex())) {
            if (seen.add(neighbor)) {
              boundary.add(neighbor);
            }
          }
        }
      }
    }
    Coordinate originCoordinate = origin.getCoordinate();
    boundary.sort(
      Comparator.comparingDouble(v ->
        SphericalDistanceLibrary.fastDistance(originCoordinate, v.getCoordinate())
      )
    );
    return boundary;
  }
}
