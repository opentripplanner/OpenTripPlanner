package org.opentripplanner.ext.carpooling.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.collection.Pair;

/**
 * The {@link CarpoolRouter} of an access/egress request once the trips carry corridors.
 * <p>
 * Segments with the passenger at one end come from the passenger's own two trees, shared by
 * every trip. Everything else, the baseline legs and the segments between a waypoint and a stop,
 * was routed when the trip arrived: the service registers those from the {@link CarpoolCorridor} as
 * plain durations before a trip is evaluated. Nothing is searched per trip at request time.
 * <p>
 * Not thread-safe; one instance per request.
 */
public class CorridorRouter implements CarpoolRouter {

  private final CarpoolTreeStreetRouter passengerRouter;
  private final Vertex passengerVertex;
  private final CarpoolStreetRouter pathRouter;
  private final Map<Pair<Vertex>, RoutedSegment> tripSegments = new HashMap<>();

  /**
   * @param passengerRouter the tree router with the passenger's vertex registered in both
   *                        directions
   * @param passengerVertex the passenger's snapped vertex
   * @param pathRouter      finds a corridor segment's path
   */
  public CorridorRouter(
    CarpoolTreeStreetRouter passengerRouter,
    Vertex passengerVertex,
    CarpoolStreetRouter pathRouter
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
    tripSegments.putIfAbsent(
      new Pair<>(from, to),
      new CorridorSegment(from, to, seconds, pathRouter)
    );
  }

  /**
   * A registered trip segment, else the passenger's trees for a segment touching the passenger,
   * else {@code null}: a pair that is neither is an insertion the corridor cannot serve. A
   * passenger snapped onto one of the trip's vertices thus gets the trip's legs from the corridor,
   * also where the passenger's trees stop short of them.
   */
  @Override
  @Nullable
  public RoutedSegment route(Vertex from, Vertex to) {
    var tripSegment = tripSegments.get(new Pair<>(from, to));
    if (tripSegment != null) {
      return tripSegment;
    }
    if (from.equals(passengerVertex) || to.equals(passengerVertex)) {
      return passengerRouter.route(from, to);
    }
    return null;
  }

  /** A duration known from the corridor; the path is found when first asked for. */
  static final class CorridorSegment implements RoutedSegment {

    private final Vertex from;
    private final Vertex to;
    private final int durationSeconds;
    private final CarpoolStreetRouter pathRouter;

    @Nullable
    private StreetPath path;

    CorridorSegment(Vertex from, Vertex to, int durationSeconds, CarpoolStreetRouter pathRouter) {
      this.from = from;
      this.to = to;
      this.durationSeconds = durationSeconds;
      this.pathRouter = pathRouter;
    }

    @Override
    public int durationSeconds() {
      return durationSeconds;
    }

    /**
     * The path between the segment's ends, found by a car street search the first time it is asked
     * for. The corridor holds a duration for the segment, so a segment that cannot be routed is an
     * error.
     */
    @Override
    public StreetPath path() {
      if (path == null) {
        var segment = pathRouter.route(from, to);
        if (segment == null) {
          throw new IllegalStateException(
            "The corridor holds a duration for " + from + " -> " + to + " but it cannot be routed"
          );
        }
        path = segment.path();
      }
      return path;
    }

    @Override
    public String toString() {
      return "CorridorSegment{" + from + " -> " + to + ", " + durationSeconds + "s}";
    }
  }
}
