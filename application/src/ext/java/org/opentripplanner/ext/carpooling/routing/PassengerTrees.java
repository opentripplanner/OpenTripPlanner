package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.BeelineEstimator.minDriveSeconds;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter.Direction;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * The passenger's two car trees of an access/egress request, outward from the passenger and inward
 * to it.
 */
public final class PassengerTrees {

  private PassengerTrees() {}

  /** A router with the passenger's vertex registered in both directions. */
  public static CarpoolTreeStreetRouter router(
    Vertex passenger,
    List<RoutableCarpoolTrip> trips,
    double maxCarSpeed,
    Duration maxTree
  ) {
    var router = new CarpoolTreeStreetRouter();
    router.addVertex(
      passenger,
      Direction.FROM,
      min(treeLimit(passenger, trips, maxCarSpeed, Direction.FROM), maxTree)
    );
    router.addVertex(
      passenger,
      Direction.TO,
      min(treeLimit(passenger, trips, maxCarSpeed, Direction.TO), maxTree)
    );
    return router;
  }

  /**
   * How far the passenger's tree has to reach. The forward tree answers {@code passenger → X} for a
   * passenger picked up (or, for egress, dropped off) inside some leg {@code k}: the driver goes
   * {@code a_k → passenger → … → a_(k+1)} in at most the leg's limit, and the stretch before the
   * passenger takes at least the beeline from {@code a_k} at the fastest speed in the graph, so the
   * tree has to cover at most the limit minus that bound. The reverse tree answers
   * {@code X → passenger} and subtracts the beeline from the passenger to {@code a_(k+1)} instead.
   * The result is the largest such value over all legs of all the trips.
   */
  private static Duration treeLimit(
    Vertex passenger,
    List<RoutableCarpoolTrip> trips,
    double maxCarSpeed,
    Direction direction
  ) {
    var limit = Duration.ZERO;
    for (var trip : trips) {
      var forTrip = treeLimit(
        passenger,
        trip.vertices(),
        trip.corridor().legLimits(),
        maxCarSpeed,
        direction
      );
      if (forTrip.compareTo(limit) > 0) {
        limit = forTrip;
      }
    }
    return limit;
  }

  /** The widest leg of one trip, floored at zero. Package-private for testing. */
  static Duration treeLimit(
    Vertex passenger,
    List<Vertex> waypoints,
    List<Duration> legLimits,
    double maxCarSpeed,
    Direction direction
  ) {
    var limit = Duration.ZERO;
    for (int leg = 0; leg < legLimits.size(); leg++) {
      var needed = legReach(passenger, waypoints, legLimits, leg, maxCarSpeed, direction);
      if (needed.compareTo(limit) > 0) {
        limit = needed;
      }
    }
    return limit;
  }

  /** The leg's limit minus the beeline the driver needs on the other side of the passenger. */
  private static Duration legReach(
    Vertex passenger,
    List<Vertex> waypoints,
    List<Duration> legLimits,
    int leg,
    double maxCarSpeed,
    Direction direction
  ) {
    var passengerCoordinate = passenger.getCoordinate();
    double beeline =
      direction == Direction.FROM
        ? minDriveSeconds(waypoints.get(leg).getCoordinate(), passengerCoordinate, maxCarSpeed)
        : minDriveSeconds(passengerCoordinate, waypoints.get(leg + 1).getCoordinate(), maxCarSpeed);
    // Rounded down, to stay a lower bound.
    return legLimits.get(leg).minusSeconds((long) beeline);
  }

  private static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }
}
