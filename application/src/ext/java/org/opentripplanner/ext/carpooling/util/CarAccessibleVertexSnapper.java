package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
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
 * Resolves a vertex a car can route through near a target location — used to pick a pickup or
 * dropoff point for a carpool when the passenger's origin/destination (or a transit stop's
 * street-linked vertex) sits on a pedestrian-only edge that the carpool driver cannot reach.
 * <p>
 * The input is one end of a leg the carpool participates in: the passenger's origin/destination
 * for a direct carpool, or a transit stop's street-linked vertex for an access/egress leg. This
 * returns the input vertex unchanged when it already has both an incoming and outgoing
 * car-permitting street edge, otherwise runs a bounded WALK A* to find the cheapest such vertex
 * by generalized walk weight (i.e. walk time scaled by the request's reluctance, safety factor,
 * and other preferences — not raw distance). If the input is already car-accessible the result
 * contains no walk path; otherwise the result carries the walk path the passenger uses to bridge
 * the pedestrian-only stretch.
 * <p>
 * The "car-accessible" check is intentionally minimal — bidirectional car-permitting street
 * edges, nothing more. Speed limits, road class, and OSM stopping/parking restrictions are not
 * consulted; OTP does not model explicit no-stopping zones today, and this matches the
 * precedent set by {@code CAR_PICKUP}, which similarly transitions on any car-permitting edge.
 * <p>
 * The bounded walk A* is capped by {@code maxWalk}; if no car-accessible vertex is reachable
 * within that budget — or no walk-traversable edge exists out of the input vertex in the search
 * direction — {@code null} is returned and the caller should reject the carpool match.
 * <p>
 * Use {@link #snapPickup} when the walker departs from {@code vertexToSnap} (forward search,
 * outgoing edges) and {@link #snapDropoff} when the walker arrives at it (reverse search,
 * incoming edges). The returned walk path is always in chronological order, from the walk's
 * start vertex to its end vertex.
 */
public final class CarAccessibleVertexSnapper {

  private CarAccessibleVertexSnapper() {}

  /**
   * Outcome of a snap: a car-accessible {@code vertex} the carpool driver can pick up or drop off
   * at, paired with the {@code walkPath} the passenger uses to bridge the pedestrian-only stretch
   * between that vertex and the original input.
   * <p>
   * {@code walkPath} is {@code null} in two cases: the input vertex was already car-accessible (no
   * walk needed), or the snap landed on a zero-duration neighbour reached only through the
   * passenger-side {@code TemporaryStreetLocation}'s zero-cost {@code TemporaryFreeEdge}s — i.e.
   * the same physical point, just a different graph node, with no real walking to render.
   * <p>
   * When non-null, the path is always in chronological order: from {@code vertexToSnap} to
   * {@code vertex} for a pickup, and from {@code vertex} to {@code vertexToSnap} for a dropoff
   * (see {@link #snapPickup} / {@link #snapDropoff}).
   */
  public record SnapResult(Vertex vertex, @Nullable GraphPath<State, Edge, Vertex> walkPath) {}

  /**
   * Snaps a pickup point: searches forward from {@code vertexToSnap} along outgoing edges to find
   * the lowest-weight car-accessible vertex within {@code maxWalk}. The returned walk path runs
   * from {@code vertexToSnap} to the snapped vertex.
   *
   * @see #snap for the parameter contract.
   */
  @Nullable
  public static SnapResult snapPickup(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk
  ) {
    return snap(baseRequest, vertexToSnap, maxWalk, false);
  }

  /**
   * Snaps a dropoff point: searches backward to {@code vertexToSnap} along incoming edges to find
   * the lowest-weight car-accessible vertex within {@code maxWalk}. The returned walk path runs
   * from the snapped vertex to {@code vertexToSnap}.
   *
   * @see #snap for the parameter contract.
   */
  @Nullable
  public static SnapResult snapDropoff(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk
  ) {
    return snap(baseRequest, vertexToSnap, maxWalk, true);
  }

  /**
   * @param baseRequest a {@link StreetSearchRequest} carrying the user's walk preferences (speed,
   *        reluctance, safety factor, …). The walk A* runs with these values; mode is forced to
   *        {@link StreetMode#WALK} and {@code arriveBy} is overridden by the {@code arriveBy}
   *        parameter, so callers can hand in any request shape (typically the result of mapping
   *        their {@code RouteRequest}). Pass {@link StreetSearchRequest#DEFAULT} only when
   *        defaults are truly intended.
   * @param vertexToSnap one end of the leg the carpool participates in: a passenger
   *        origin/destination for a direct carpool, or a transit stop's street-linked vertex for
   *        an access/egress leg. The function returns this vertex unchanged when it is already
   *        car-accessible, otherwise searches outward (or inward, when {@code arriveBy}) for the
   *        lowest-weight car-accessible vertex.
   * @param maxWalk walking-time budget for reaching a car-accessible vertex.
   * @param arriveBy {@code false} for a pickup (walker departs from {@code vertexToSnap}),
   *        {@code true} for a dropoff (walker arrives at it).
   * @return the snap result, or {@code null} if no car-accessible vertex is reachable within
   *         {@code maxWalk}.
   */
  @Nullable
  private static SnapResult snap(
    StreetSearchRequest baseRequest,
    Vertex vertexToSnap,
    Duration maxWalk,
    boolean arriveBy
  ) {
    if (isCarAccessible(vertexToSnap)) {
      return new SnapResult(vertexToSnap, null);
    }

    // We want the cheapest car-accessible vertex within maxWalk of the input. A* isn't built for
    // "first vertex matching a predicate" — it normally terminates at a fixed target or once it has
    // explored the full walk shed. So we repurpose its termination strategy as our predicate:
    // the strategy is called once per expanded state in cost-ascending order (this ordering
    // depends on the dominance function and heuristic configured below — see the package
    // discussion for why), so the first state we see at a car-accessible vertex is guaranteed to
    // be the cheapest one reachable. The strategy only returns a boolean, so we stash the winning
    // state in foundRef on the side and read it back after the search returns. The one-element
    // array is the standard Java escape hatch for mutating a captured local from a lambda — the
    // search runs single-threaded so no synchronization is needed.
    State[] foundRef = new State[1];
    SearchTerminationStrategy<State> terminator = state -> {
      if (isCarAccessible(state.getVertex())) {
        foundRef[0] = state;
        return true;
      }
      return false;
    };

    var request = StreetSearchRequest.copyOf(baseRequest)
      .withMode(StreetMode.WALK)
      .withArriveBy(arriveBy)
      .build();
    // Pin the heuristic explicitly: A* expands states in order of f = g + h, where g is the
    // actual cost from the start, h is the heuristic's estimate of the remaining cost to the
    // goal, and f is the resulting priority used to pick the next state. The termination
    // strategy fires per state in cost-ascending order only when expansion is by g alone —
    // i.e. when h = 0 and so f == g. The trivial heuristic returns 0 for every state, which
    // satisfies that. Any other heuristic would let a state with smaller g be expanded after a
    // state with larger g but smaller h, breaking the "first match is cheapest" guarantee.
    var builder = StreetSearchBuilder.of()
      .withRequest(request)
      .withHeuristic(RemainingWeightHeuristic.TRIVIAL)
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(maxWalk))
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withTerminationStrategy(terminator);
    // AStarBuilder picks the initial-state vertex set based on arriveBy: toVertices for a reverse
    // search, fromVertices for a forward one. Route vertexToSnap to the matching side or
    // createInitialStates receives null.
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
    // The passenger-side TemporaryStreetLocation links to its split vertices via zero-cost
    // TemporaryFreeEdges; if the snap landed on one of those, there's no real walk to render.
    if (path.getDuration() == 0) {
      return new SnapResult(best.getVertex(), null);
    }
    return new SnapResult(best.getVertex(), path);
  }

  /**
   * A vertex is "car-accessible" for the purposes of this snapper when there is at least one
   * incoming car-permitting street edge <em>and</em> at least one outgoing car-permitting street
   * edge — i.e. a car can both arrive at it and leave it. Both directions are required because
   * carpool pickups and dropoffs are mid-route insertions; a vertex with only one (e.g. the
   * pedestrian-side end of a one-way drivable exit) is unrouteable for the carpool A*.
   */
  private static boolean isCarAccessible(Vertex vertex) {
    return (
      anyStreetEdgeAllowsCar(vertex.getIncoming()) && anyStreetEdgeAllowsCar(vertex.getOutgoing())
    );
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
