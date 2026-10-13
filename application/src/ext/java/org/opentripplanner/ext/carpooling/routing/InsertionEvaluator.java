package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.BeelineEstimator.minDriveSeconds;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.Pair;

/**
 * Finds, for a passenger's pickup and dropoff points, the best place to insert them into a
 * carpool trip: the pair of legs (one for the pickup, one for the dropoff, possibly the same) that
 * adds the least driving time while keeping every later stop within its deviation budget and
 * every leg the passenger rides within the car's capacity.
 * <p>
 * The cost of an insertion decomposes when the two points go into different legs: putting a point
 * into leg {@code k} adds {@code drive(leg start → point) + drive(point → leg end) − leg + dwell},
 * independent of where the other point goes. Into the same leg the driver goes from the leg's start
 * to the pickup, the dropoff and the leg's end, with the passenger's ride in between. Every later
 * stop is delayed by the pickup's detour, and by the dropoff's
 * too once past the dropoff, so the delay constraint reduces to comparing each detour with the
 * smallest budget among the stops it delays. Per trip the budget range minima and the capacity per
 * pair of legs are computed once, and the legs' driving times are the trip's corridor's; per point
 * the two driving times of a leg are routed the first time a pair needs them. A pair is then a few
 * additions and comparisons, so the number of stops on the trip no longer matters, and only the
 * winner's route is assembled. The passenger's own ride, pickup to dropoff, is routed once per
 * request.
 * <p>
 * A leg is not routed at all when the beeline detour through it, at the fastest speed in the
 * graph, already exceeds the smallest budget of the stops after it, and a segment into or out of
 * a leg is searched no further than the leg's limit: a longer one cannot be part of an insertion
 * within the budgets. With a goal-directed router (direct mode) this avoids the searches that
 * fail only after exploring everything within the trip's time bound.
 * <p>
 * One evaluator serves one request on one thread. Everything works on segment durations; no
 * street path is built here, see {@link RoutedSegment}.
 */
public class InsertionEvaluator {

  private final CarpoolRouter carpoolRouter;
  private final CarpoolStreetRouter pathRouter;
  private final Duration stopDuration;
  private final double maxCarSpeed;
  private final Map<Pair<Vertex>, Optional<RoutedSegment>> rides = new HashMap<>();

  /**
   * @param carpoolRouter routes the segments with the passenger at one end
   * @param pathRouter finds the path of a leg the passenger rides through
   * @param stopDuration dwell added at each intermediate stop, the passenger's included
   * @param maxCarSpeed the fastest speed in the graph in m/s, for the beeline pre-check
   */
  public InsertionEvaluator(
    CarpoolRouter carpoolRouter,
    CarpoolStreetRouter pathRouter,
    Duration stopDuration,
    double maxCarSpeed
  ) {
    if (!(maxCarSpeed > 0)) {
      throw new IllegalArgumentException("maxCarSpeed must be positive, got " + maxCarSpeed);
    }
    this.carpoolRouter = carpoolRouter;
    this.pathRouter = pathRouter;
    this.stopDuration = stopDuration;
    this.maxCarSpeed = maxCarSpeed;
  }

  /**
   * @return A list containing the best insertion that can be found for every stop in
   * the list tripWithViableAccessEgress.viableAccessEgress. If there are no valid insertions
   * for a stop, then no candidate for that stop will be returned.
   */
  public List<InsertionCandidate> findBestInsertions(
    TripWithViableAccessEgress tripWithViableAccessEgress
  ) {
    var viableStops = tripWithViableAccessEgress.viableAccessEgress();
    if (viableStops.isEmpty()) {
      return List.of();
    }
    var plan = plan(tripWithViableAccessEgress.routableTrip());
    var candidates = new ArrayList<InsertionCandidate>();
    for (var viable : viableStops) {
      var candidate = plan.best(toPassengerSnap(viable), viable.transitStop());
      if (candidate != null) {
        candidates.add(candidate);
      }
    }
    return candidates;
  }

  /**
   * The best insertion of one passenger ride into the trip, or {@code null} if no pair of legs can
   * take it within the budgets and the capacity.
   *
   * @param snap pickup/dropoff vertices (already snapped to car-reachable vertices by the caller)
   *        and the optional walk paths bracketing the carpool ride
   */
  @Nullable
  public InsertionCandidate findBestInsertion(
    RoutableCarpoolTrip routableTrip,
    PassengerSnap snap
  ) {
    return plan(routableTrip).best(snap, null);
  }

  /** The baseline legs come from the trip's corridor; their paths are found when needed. */
  private TripInsertionPlan plan(RoutableCarpoolTrip routableTrip) {
    var waypoints = routableTrip.vertices();
    var legDurations = routableTrip.corridor().legDurations();
    var baseline = new RoutedSegment[legDurations.size()];
    for (int leg = 0; leg < baseline.length; leg++) {
      baseline[leg] = new CorridorRouter.CorridorSegment(
        waypoints.get(leg),
        waypoints.get(leg + 1),
        (int) legDurations.get(leg).toSeconds(),
        pathRouter
      );
    }
    return new TripInsertionPlan(routableTrip, baseline);
  }

  /** The passenger's own ride, pickup → dropoff, routed once per request. */
  @Nullable
  private RoutedSegment ride(Vertex pickup, Vertex dropoff) {
    return rides
      .computeIfAbsent(new Pair<>(pickup, dropoff), pair ->
        Optional.ofNullable(carpoolRouter.route(pickup, dropoff))
      )
      .orElse(null);
  }

  private static PassengerSnap toPassengerSnap(ViableAccessEgress viable) {
    boolean access = viable.accessEgress() == AccessEgressType.ACCESS;
    return new PassengerSnap(
      access ? viable.passengerVertex() : viable.transitVertex(),
      access ? viable.transitVertex() : viable.passengerVertex(),
      viable.walkToPickup(),
      viable.walkFromDropoff()
    );
  }

  /**
   * Everything about one trip that every insertion into it shares. Leg {@code k} runs from
   * waypoint {@code k} to waypoint {@code k + 1}; a pickup in leg {@code i} is route position
   * {@code i + 1} and a dropoff in leg {@code j} position {@code j + 2}, in the conventions of
   * {@link InsertionCandidate}.
   */
  private final class TripInsertionPlan {

    private final RoutableCarpoolTrip routableTrip;
    private final RoutedSegment[] baseline;
    private final long[] legSeconds;
    private final long dwell;
    /** {@code minBudget[a][b]}: the smallest deviation budget of the stops a..b (inclusive). */
    private final long[][] minBudget;
    /** {@code capacityOk[i][j]}: one more passenger fits on every leg i..j. */
    private final boolean[][] capacityOk;
    private final Map<Vertex, LegTimes> legTimesByVertex = new HashMap<>();

    TripInsertionPlan(RoutableCarpoolTrip routableTrip, RoutedSegment[] baseline) {
      this.routableTrip = routableTrip;
      this.baseline = baseline;
      this.dwell = stopDuration.toSeconds();
      int legs = baseline.length;
      this.legSeconds = new long[legs];
      for (int k = 0; k < legs; k++) {
        legSeconds[k] = baseline[k].durationSeconds();
      }
      CarpoolTrip trip = routableTrip.trip();
      int points = legs + 1;
      this.minBudget = new long[points][points];
      for (int a = 1; a < points; a++) {
        long min = Long.MAX_VALUE;
        for (int b = a; b < points; b++) {
          min = Math.min(min, trip.stops().get(b).getDeviationBudget().toSeconds());
          minBudget[a][b] = min;
        }
      }
      this.capacityOk = new boolean[legs][legs];
      for (int i = 0; i < legs; i++) {
        boolean ok = true;
        for (int j = i; j < legs && ok; j++) {
          ok = trip.getPassengerCountAtDepartureOfStop(j) + 1 <= trip.totalCapacity();
          capacityOk[i][j] = ok;
        }
      }
    }

    /** The smallest budget among stops a..b, or no constraint when the range is empty. */
    private long budget(int a, int b) {
      return a > b ? Long.MAX_VALUE : minBudget[a][b];
    }

    @Nullable
    InsertionCandidate best(PassengerSnap snap, @Nullable StopLocation transitStop) {
      Vertex pickup = snap.pickupVertex();
      Vertex dropoff = snap.dropoffVertex();
      LegTimes pickupTimes = legTimes(pickup);
      LegTimes dropoffTimes = legTimes(dropoff);
      int legs = legSeconds.length;
      int lastStop = legs;
      long bestExtra = Long.MAX_VALUE;
      int bestPickupLeg = -1;
      int bestDropoffLeg = -1;

      for (int i = 0; i < legs; i++) {
        if (!capacityOk[i][i]) {
          // The car is full on leg i, so no dropoff leg works with a pickup in it.
          continue;
        }
        RoutedSegment toPickup = pickupTimes.in(i);
        if (toPickup == null) {
          continue;
        }
        for (int j = i; j < legs; j++) {
          if (!capacityOk[i][j]) {
            // The car is full somewhere on legs i..j, so on every longer span too.
            break;
          }
          long extra;
          if (i == j) {
            // Pickup and dropoff in the same leg: leg start → pickup → dropoff → leg end.
            RoutedSegment fromDropoff = dropoffTimes.out(i);
            if (fromDropoff == null) {
              continue;
            }
            RoutedSegment ride = ride(pickup, dropoff);
            if (ride == null) {
              continue;
            }
            extra =
              toPickup.durationSeconds() +
              ride.durationSeconds() +
              fromDropoff.durationSeconds() -
              legSeconds[i] +
              2 * dwell;
            if (extra > budget(i + 1, lastStop)) {
              continue;
            }
          } else {
            RoutedSegment fromPickup = pickupTimes.out(i);
            if (fromPickup == null) {
              break;
            }
            long pickupDetour =
              toPickup.durationSeconds() + fromPickup.durationSeconds() - legSeconds[i] + dwell;
            if (pickupDetour > budget(i + 1, j)) {
              // Every later dropoff leg delays these same stops by at least as much.
              break;
            }
            RoutedSegment toDropoff = dropoffTimes.in(j);
            if (toDropoff == null) {
              continue;
            }
            RoutedSegment fromDropoff = dropoffTimes.out(j);
            if (fromDropoff == null) {
              continue;
            }
            long dropoffDetour =
              toDropoff.durationSeconds() + fromDropoff.durationSeconds() - legSeconds[j] + dwell;
            extra = pickupDetour + dropoffDetour;
            if (extra > budget(j + 1, lastStop)) {
              continue;
            }
          }
          if (extra < bestExtra) {
            bestExtra = extra;
            bestPickupLeg = i;
            bestDropoffLeg = j;
          }
        }
      }
      if (bestPickupLeg < 0) {
        return null;
      }
      var ride = bestPickupLeg == bestDropoffLeg ? ride(pickup, dropoff) : null;
      return new InsertionCandidate(
        routableTrip.trip(),
        bestPickupLeg + 1,
        bestDropoffLeg + 2,
        routeWith(bestPickupLeg, bestDropoffLeg, pickupTimes, dropoffTimes, ride),
        stopDuration,
        transitStop,
        snap.walkToPickup(),
        snap.walkFromDropoff()
      );
    }

    /** The trip's segments with the passenger's pickup in leg i and dropoff in leg j. */
    private List<RoutedSegment> routeWith(
      int i,
      int j,
      LegTimes pickupTimes,
      LegTimes dropoffTimes,
      @Nullable RoutedSegment ride
    ) {
      var segments = new ArrayList<RoutedSegment>(baseline.length + 2);
      for (int k = 0; k < baseline.length; k++) {
        if (k == i && k == j) {
          segments.add(Objects.requireNonNull(pickupTimes.in(k)));
          segments.add(Objects.requireNonNull(ride));
          segments.add(Objects.requireNonNull(dropoffTimes.out(k)));
        } else if (k == i) {
          segments.add(Objects.requireNonNull(pickupTimes.in(k)));
          segments.add(Objects.requireNonNull(pickupTimes.out(k)));
        } else if (k == j) {
          segments.add(Objects.requireNonNull(dropoffTimes.in(k)));
          segments.add(Objects.requireNonNull(dropoffTimes.out(k)));
        } else {
          segments.add(baseline[k]);
        }
      }
      return segments;
    }

    /** The driving times into and out of the legs for a point, kept for the rest of the trip. */
    private LegTimes legTimes(Vertex point) {
      return legTimesByVertex.computeIfAbsent(point, LegTimes::new);
    }

    /**
     * Per leg, the segment from the leg's start to a point ({@code in}) and from the point to the
     * leg's end ({@code out}), each routed the first time it is asked for; {@code null} means
     * unroutable, or ruled out by the beeline pre-check.
     */
    private final class LegTimes {

      private final Vertex point;
      private final RoutedSegment[] in = new RoutedSegment[legSeconds.length];
      private final RoutedSegment[] out = new RoutedSegment[legSeconds.length];
      private final boolean[] inRouted = new boolean[legSeconds.length];
      private final boolean[] outRouted = new boolean[legSeconds.length];

      LegTimes(Vertex point) {
        this.point = point;
      }

      @Nullable
      RoutedSegment in(int leg) {
        if (!inRouted[leg]) {
          inRouted[leg] = true;
          if (withinReach(leg)) {
            in[leg] = carpoolRouter.route(routableTrip.vertices().get(leg), point, limit(leg));
          }
        }
        return in[leg];
      }

      @Nullable
      RoutedSegment out(int leg) {
        if (!outRouted[leg]) {
          outRouted[leg] = true;
          if (withinReach(leg)) {
            out[leg] = carpoolRouter.route(point, routableTrip.vertices().get(leg + 1), limit(leg));
          }
        }
        return out[leg];
      }

      private Duration limit(int leg) {
        return routableTrip.corridor().legLimits().get(leg);
      }

      /**
       * Whether the beeline detour through the point, at the fastest speed in the graph, fits the
       * smallest budget of the stops after the leg, all of which it delays: a lower bound on the
       * real detour.
       */
      private boolean withinReach(int leg) {
        var waypoints = routableTrip.vertices();
        var p = point.getCoordinate();
        double beeline =
          minDriveSeconds(waypoints.get(leg).getCoordinate(), p, maxCarSpeed) +
          minDriveSeconds(p, waypoints.get(leg + 1).getCoordinate(), maxCarSpeed);
        double detour = beeline - legSeconds[leg] + dwell;
        return detour <= budget(leg + 1, legSeconds.length);
      }
    }
  }
}
