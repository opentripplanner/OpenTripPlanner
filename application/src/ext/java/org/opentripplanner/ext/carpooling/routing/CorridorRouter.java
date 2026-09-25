package org.opentripplanner.ext.carpooling.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.collection.Pair;

/**
 * The {@link CarpoolRouter} of an access/egress request once the trips carry corridors.
 * <p>
 * Segments with the passenger at one end come from the passenger's own two trees, shared by every
 * trip. Everything else, the baseline legs and the segments between a waypoint and a stop, was
 * routed when the trip arrived: the service registers those from the {@link CarpoolCorridor} as
 * plain durations before a trip is evaluated. Nothing is searched per trip at request time.
 * <p>
 * A corridor segment's path, needed only for the few insertions Raptor keeps, is routed
 * goal-directed on demand with the path router.
 * <p>
 * Not thread-safe; one instance per request.
 */
public class CorridorRouter implements CarpoolRouter {

  private final CarpoolTreeStreetRouter passengerRouter;
  private final Vertex passengerVertex;
  private final CarpoolRouter pathRouter;
  private final Map<Pair<Vertex>, RoutedSegment> tripSegments = new HashMap<>();

  /**
   * @param passengerRouter the tree router with the passenger's vertex registered in both
   *        directions
   * @param passengerVertex the passenger's snapped vertex
   * @param pathRouter goal-directed router that builds the path of a corridor segment on demand
   */
  public CorridorRouter(
    CarpoolTreeStreetRouter passengerRouter,
    Vertex passengerVertex,
    CarpoolRouter pathRouter
  ) {
    this.passengerRouter = Objects.requireNonNull(passengerRouter);
    this.passengerVertex = Objects.requireNonNull(passengerVertex);
    this.pathRouter = Objects.requireNonNull(pathRouter);
  }

  /** Forgets the previous trip's segments. */
  public void beginTrip() {
    tripSegments.clear();
  }

  /** Registers a segment of the current trip with the duration known from its corridor. */
  public void register(Vertex from, Vertex to, int seconds) {
    tripSegments.putIfAbsent(new Pair<>(from, to), new CorridorSegment(from, to, seconds));
  }

  /** Number of registered trip segments. Package-private for testing. */
  int tripSegmentCount() {
    return tripSegments.size();
  }

  /**
   * The passenger's trees for a segment touching the passenger, else a registered trip segment,
   * else {@code null}: a pair that is neither is an insertion the corridor cannot serve.
   */
  @Override
  @Nullable
  public RoutedSegment route(Vertex from, Vertex to) {
    if (from.equals(passengerVertex) || to.equals(passengerVertex)) {
      return passengerRouter.route(from, to);
    }
    return tripSegments.get(new Pair<>(from, to));
  }

  /** A duration known from the corridor; the path is routed goal-directed when first asked for. */
  private final class CorridorSegment implements RoutedSegment {

    private final Vertex from;
    private final Vertex to;
    private final int durationSeconds;

    @Nullable
    private GraphPath<State, Edge, Vertex> path;

    CorridorSegment(Vertex from, Vertex to, int durationSeconds) {
      this.from = from;
      this.to = to;
      this.durationSeconds = durationSeconds;
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
        var routed = pathRouter.route(from, to);
        if (routed == null) {
          throw new IllegalStateException(
            "The corridor holds a duration for " + from + " -> " + to + " but it cannot be routed"
          );
        }
        path = routed.path();
      }
      return path;
    }

    @Override
    public String toString() {
      return "CorridorSegment{" + from + " -> " + to + ", " + durationSeconds + "s}";
    }
  }
}
