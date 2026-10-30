package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.BeelineEstimator.minDriveSeconds;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter.Direction;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * The passenger's car trees of a request, each outward from one of the passenger's points or inward
 * to it. A tree only has to reach as far as the trips' legs allow: in a leg, the driver also drives
 * from the leg's start to the point (outward) or from the point to the leg's end (inward), which
 * takes at least the beeline at the fastest speed in the graph, so the tree reaches no further than
 * the leg's limit minus that drive.
 * <p>
 * A tree that serves the segments between the point and the trips' waypoints stops once it has
 * them. The tree on the side of an access/egress passenger's ride stops at the longest ride the
 * request allows.
 */
public final class PassengerTrees {

  private final List<RoutableCarpoolTrip> trips;
  private final double maxCarSpeed;
  private final Duration maxTree;

  PassengerTrees(List<RoutableCarpoolTrip> trips, double maxCarSpeed, Duration maxTree) {
    this.trips = trips;
    this.maxCarSpeed = maxCarSpeed;
    this.maxTree = maxTree;
  }

  /**
   * A router with trees outward from and inward to both the pickup and the dropoff. The pickup's
   * outward tree also has to reach the dropoff, for the passenger's ride.
   */
  public static CarpoolTreeStreetRouter directRouter(
    Vertex pickup,
    Vertex dropoff,
    List<RoutableCarpoolTrip> trips,
    double maxCarSpeed,
    Duration maxTree
  ) {
    var trees = new PassengerTrees(trips, maxCarSpeed, maxTree);
    var router = new CarpoolTreeStreetRouter();

    var pickupOutwardLimit = trees.limit(pickup, Direction.FROM);
    var pickupOutwardTargets = trees.waypointReach(pickup, Direction.FROM, true);
    pickupOutwardTargets.put(dropoff, pickupOutwardLimit);
    router.addVertex(pickup, Direction.FROM, pickupOutwardLimit, pickupOutwardTargets);

    trees.addTree(router, pickup, Direction.TO, true);
    trees.addTree(router, dropoff, Direction.FROM, false);
    trees.addTree(router, dropoff, Direction.TO, false);
    return router;
  }

  /**
   * A router with trees outward from and inward to the passenger of an access/egress request. An
   * access passenger is picked up: the driver comes in to the passenger, who then rides out. An
   * egress passenger is dropped off: the passenger rides in, and the driver goes on out. Only the
   * tree on the driver's side stops once it has reached the trips' waypoints.
   *
   * @param maxRide the longest ride the request allows
   */
  public static CarpoolTreeStreetRouter accessEgressRouter(
    Vertex passenger,
    List<RoutableCarpoolTrip> trips,
    boolean access,
    double maxCarSpeed,
    Duration maxTree,
    Duration maxRide
  ) {
    var trees = new PassengerTrees(trips, maxCarSpeed, maxTree);
    var router = new CarpoolTreeStreetRouter();

    var driverSide = access ? Direction.TO : Direction.FROM;
    trees.addTree(router, passenger, driverSide, access);

    var rideSide = access ? Direction.FROM : Direction.TO;
    router.addVertex(passenger, rideSide, min(trees.limit(passenger, rideSide), maxRide));
    return router;
  }

  private void addTree(
    CarpoolTreeStreetRouter router,
    Vertex point,
    Direction direction,
    boolean pickup
  ) {
    var limit = limit(point, direction);
    router.addVertex(point, direction, limit, waypointReach(point, direction, pickup));
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

  /**
   * How far the point's tree has to reach for each waypoint on the far side of the point, the leg's
   * end outward and its start inward: the reach of that leg. A pickup never needs the trip's last
   * stop, nor a dropoff its first: the passenger rides from the pickup to the dropoff.
   * Package-private for testing.
   *
   * @param pickup whether the point is a pickup, else a dropoff
   */
  Map<Vertex, Duration> waypointReach(Vertex point, Direction direction, boolean pickup) {
    var reach = new HashMap<Vertex, Duration>();
    for (var trip : trips) {
      var stops = trip.vertices();
      int stopNeverNeeded = pickup ? stops.size() - 1 : 0;
      for (int leg = 0; leg < trip.corridor().legCount(); leg++) {
        int farStop = direction == Direction.FROM ? leg + 1 : leg;
        var legReach = legReach(trip, leg, point, direction);
        if (farStop != stopNeverNeeded && !legReach.isNegative()) {
          reach.merge(stops.get(farStop), legReach, PassengerTrees::max);
        }
      }
    }
    return reach;
  }

  /** The leg's limit minus the drive on the other side of the point. */
  private Duration legReach(RoutableCarpoolTrip trip, int leg, Vertex point, Direction direction) {
    var stops = trip.vertices();
    var otherSide =
      direction == Direction.FROM
        ? minDrive(stops.get(leg), point)
        : minDrive(point, stops.get(leg + 1));
    return trip.corridor().legLimits().get(leg).minus(otherSide);
  }

  /** The beeline drive at the fastest speed in the graph, rounded down to stay a lower bound. */
  private Duration minDrive(Vertex from, Vertex to) {
    return Duration.ofSeconds(
      (long) minDriveSeconds(from.getCoordinate(), to.getCoordinate(), maxCarSpeed)
    );
  }

  private static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  private static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
  }
}
