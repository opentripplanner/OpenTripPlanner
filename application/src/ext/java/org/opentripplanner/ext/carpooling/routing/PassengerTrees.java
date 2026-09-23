package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter.Direction;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * The passenger's car trees of a request, each outward from one of the passenger's points or inward
 * to it. A tree only has to reach as far as the trips' legs allow: in a leg, the driver also drives
 * from the leg's start to the point (outward) or from the point to the leg's end (inward), which
 * takes at least the beeline at the fastest speed in the graph, so the tree reaches no further than
 * the leg's limit minus that drive ({@link DetourBound}).
 */
public final class PassengerTrees {

  private final List<RoutableCarpoolTrip> trips;
  private final Duration maxTree;

  PassengerTrees(List<RoutableCarpoolTrip> trips, Duration maxTree) {
    this.trips = trips;
    this.maxTree = maxTree;
  }

  /** A router with trees outward from and inward to both the pickup and the dropoff. */
  public static CarpoolTreeStreetRouter directRouter(
    Vertex pickup,
    Vertex dropoff,
    List<RoutableCarpoolTrip> trips,
    Duration maxTree
  ) {
    var trees = new PassengerTrees(trips, maxTree);
    var router = new CarpoolTreeStreetRouter();
    for (var point : List.of(pickup, dropoff)) {
      trees.addTree(router, point, Direction.FROM);
      trees.addTree(router, point, Direction.TO);
    }
    return router;
  }

  /** A router with trees outward from and inward to the passenger of an access/egress request. */
  public static CarpoolTreeStreetRouter accessEgressRouter(
    Vertex passenger,
    List<RoutableCarpoolTrip> trips,
    Duration maxTree
  ) {
    var trees = new PassengerTrees(trips, maxTree);
    var router = new CarpoolTreeStreetRouter();
    trees.addTree(router, passenger, Direction.FROM);
    trees.addTree(router, passenger, Direction.TO);
    return router;
  }

  private void addTree(CarpoolTreeStreetRouter router, Vertex point, Direction direction) {
    router.addVertex(point, direction, limit(point, direction));
  }

  /**
   * How far the point's tree has to reach: the most any leg of the trips allows, never negative and
   * at most the longest tree. Package-private for testing.
   */
  Duration limit(Vertex point, Direction direction) {
    var limit = Duration.ZERO;
    for (var trip : trips) {
      for (int leg = 0; leg < trip.corridor().legCount(); leg++) {
        limit = max(limit, legReach(trip, leg, point, direction));
      }
    }
    return min(limit, maxTree);
  }

  /** The most the leg may take from the point on (outward) or up to it (inward). */
  private Duration legReach(RoutableCarpoolTrip trip, int leg, Vertex point, Direction direction) {
    var bound = trip.corridor().legBound(leg);
    return direction == Direction.FROM ? bound.limitFrom(point) : bound.limitTo(point);
  }

  private static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  private static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
  }
}
