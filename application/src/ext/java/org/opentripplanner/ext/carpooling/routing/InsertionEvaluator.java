package org.opentripplanner.ext.carpooling.routing;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the best insertion of a passenger into a carpool trip: the pickup leg and dropoff leg that
 * add the least driving while every later stop stays within its deviation budget and a seat is free
 * on every leg the passenger rides.
 * <p>
 * A point's detour in a leg is the drive via the point, minus the leg, plus the dwell. The
 * pickup's detour delays every stop after the pickup, and both detours every stop after the
 * dropoff.
 * <p>
 * Not thread-safe; one instance per request.
 */
public class InsertionEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(InsertionEvaluator.class);

  private final CarpoolRouter carpoolRouter;
  private final Duration stopDuration;
  private final Map<Pair<Vertex>, Optional<RoutedSegment>> pickupToDropoffSegments =
    new HashMap<>();

  /**
   * @param carpoolRouter routes a street segment between two vertices of the candidate route
   * @param stopDuration  dwell added at each intermediate stop, the passenger's included
   */
  public InsertionEvaluator(CarpoolRouter carpoolRouter, Duration stopDuration) {
    this.carpoolRouter = carpoolRouter;
    this.stopDuration = stopDuration;
  }

  /**
   * @return A list containing the best insertion that can be found for every stop in the list
   *         tripWithViableAccessEgress.viableAccessEgress. If there are no valid insertions for a
   *         stop, then no candidate for that stop will be returned.
   */
  public List<InsertionCandidate> findBestInsertions(
    TripWithViableAccessEgress tripWithViableAccessEgress
  ) {
    var viableStops = tripWithViableAccessEgress.viableAccessEgress();
    if (viableStops.isEmpty()) {
      return List.of();
    }
    var plan = plan(tripWithViableAccessEgress.routableTrip());
    if (plan == null) {
      return List.of();
    }
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
   *             and the optional walk paths bracketing the carpool ride
   */
  @Nullable
  public InsertionCandidate findBestInsertion(
    RoutableCarpoolTrip routableTrip,
    PassengerSnap snap
  ) {
    var plan = plan(routableTrip);
    return plan == null ? null : plan.best(snap, null);
  }

  @Nullable
  private TripInsertionPlan plan(RoutableCarpoolTrip routableTrip) {
    var waypoints = routableTrip.vertices();
    var baseline = new RoutedSegment[waypoints.size() - 1];
    for (int leg = 0; leg < baseline.length; leg++) {
      baseline[leg] = carpoolRouter.route(waypoints.get(leg), waypoints.get(leg + 1));
      if (baseline[leg] == null) {
        LOG.info("Could not route leg {} of carpool trip {}", leg, routableTrip.trip().getId());
        return null;
      }
    }
    return new TripInsertionPlan(routableTrip, baseline);
  }

  /** The passenger's own ride, pickup → dropoff, routed once per request. */
  @Nullable
  private RoutedSegment pickupToDropoff(Vertex pickup, Vertex dropoff) {
    return pickupToDropoffSegments
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
   * Everything about one trip that every insertion into it shares. Legs and waypoints are numbered
   * from 0, and a leg runs from the waypoint with its number to the next one. A pickup in a leg is
   * route position {@code leg + 1} and a dropoff route position {@code leg + 2}, in the conventions
   * of {@link InsertionCandidate}.
   */
  private final class TripInsertionPlan {

    /** The trip the passenger is inserted into. */
    private final RoutableCarpoolTrip routableTrip;
    /** The trip's own legs, one segment per leg, used unchanged where the passenger is not. */
    private final RoutedSegment[] baseline;
    /** The number of legs in the trip. */
    private final int legCount;
    /** The driving time of each leg in seconds. */
    private final long[] legSeconds;
    /** The time in seconds the driver stops for each pickup and dropoff. */
    private final long dwell;
    /**
     * {@code budgetFrom[stop]}: the smallest deviation budget of the stops from {@code stop} to the
     * destination, for every stop after the origin.
     */
    private final long[] budgetFrom;
    /**
     * {@code capacityOk[firstLeg][lastLeg]}: one more passenger fits on every leg from
     * {@code firstLeg} to {@code lastLeg}, both included.
     */
    private final boolean[][] capacityOk;
    /** The driving times into and out of the legs for each pickup or dropoff point tried. */
    private final Map<Vertex, LegTimes> legTimesByVertex = new HashMap<>();

    TripInsertionPlan(RoutableCarpoolTrip routableTrip, RoutedSegment[] baseline) {
      this.routableTrip = routableTrip;
      this.baseline = baseline;
      this.dwell = stopDuration.toSeconds();
      this.legCount = baseline.length;
      this.legSeconds = new long[legCount];
      for (int leg = 0; leg < legCount; leg++) {
        legSeconds[leg] = baseline[leg].durationSeconds();
      }
      CarpoolTrip trip = routableTrip.trip();
      int stopCount = legCount + 1;
      this.budgetFrom = new long[stopCount];
      long min = Long.MAX_VALUE;
      for (int stop = stopCount - 1; stop >= 1; stop--) {
        min = Math.min(min, trip.stops().get(stop).getDeviationBudget().toSeconds());
        budgetFrom[stop] = min;
      }
      this.capacityOk = new boolean[legCount][legCount];
      for (int firstLeg = 0; firstLeg < legCount; firstLeg++) {
        boolean ok = true;
        for (int lastLeg = firstLeg; lastLeg < legCount && ok; lastLeg++) {
          ok = trip.getPassengerCountAtDepartureOfStop(lastLeg) + 1 <= trip.totalCapacity();
          capacityOk[firstLeg][lastLeg] = ok;
        }
      }
    }

    @Nullable
    InsertionCandidate best(PassengerSnap snap, @Nullable StopLocation transitStop) {
      Vertex pickup = snap.pickupVertex();
      Vertex dropoff = snap.dropoffVertex();
      LegTimes pickupTimes = legTimes(pickup);
      LegTimes dropoffTimes = legTimes(dropoff);
      long bestDetour = Long.MAX_VALUE;
      int bestPickupLeg = -1;
      int bestDropoffLeg = -1;

      for (int pickupLeg = 0; pickupLeg < legCount; pickupLeg++) {
        if (!capacityOk[pickupLeg][pickupLeg]) {
          // The car is full on the pickup's leg, so no dropoff leg works with a pickup in it.
          continue;
        }
        RoutedSegment toPickup = pickupTimes.fromLegStart(pickupLeg);
        if (toPickup == null) {
          continue;
        }
        for (int dropoffLeg = pickupLeg; dropoffLeg < legCount; dropoffLeg++) {
          if (!capacityOk[pickupLeg][dropoffLeg]) {
            // The car is full somewhere between the two legs, so on every longer span too.
            break;
          }
          long detour;
          if (pickupLeg == dropoffLeg) {
            // Pickup and dropoff in the same leg: leg start → pickup → dropoff → leg end.
            RoutedSegment fromDropoff = dropoffTimes.toLegEnd(pickupLeg);
            if (fromDropoff == null) {
              continue;
            }
            RoutedSegment pickupToDropoff = pickupToDropoff(pickup, dropoff);
            if (pickupToDropoff == null) {
              continue;
            }
            detour =
              toPickup.durationSeconds() +
              pickupToDropoff.durationSeconds() +
              fromDropoff.durationSeconds() -
              legSeconds[pickupLeg] +
              2 * dwell;
            if (detour > budgetFrom[pickupLeg + 1]) {
              continue;
            }
          } else {
            RoutedSegment fromPickup = pickupTimes.toLegEnd(pickupLeg);
            if (fromPickup == null) {
              break;
            }
            long pickupDetour =
              toPickup.durationSeconds() +
              fromPickup.durationSeconds() -
              legSeconds[pickupLeg] +
              dwell;
            if (pickupDetour > budgetFrom[pickupLeg + 1]) {
              // The pickup's detour delays every later stop, so no later dropoff leg works either.
              break;
            }
            RoutedSegment toDropoff = dropoffTimes.fromLegStart(dropoffLeg);
            if (toDropoff == null) {
              continue;
            }
            RoutedSegment fromDropoff = dropoffTimes.toLegEnd(dropoffLeg);
            if (fromDropoff == null) {
              continue;
            }
            long dropoffDetour =
              toDropoff.durationSeconds() +
              fromDropoff.durationSeconds() -
              legSeconds[dropoffLeg] +
              dwell;
            detour = pickupDetour + dropoffDetour;
            if (detour > budgetFrom[dropoffLeg + 1]) {
              continue;
            }
          }
          if (detour < bestDetour) {
            bestDetour = detour;
            bestPickupLeg = pickupLeg;
            bestDropoffLeg = dropoffLeg;
          }
        }
      }
      if (bestPickupLeg < 0) {
        return null;
      }
      var pickupToDropoff =
        bestPickupLeg == bestDropoffLeg ? pickupToDropoff(pickup, dropoff) : null;
      return new InsertionCandidate(
        routableTrip.trip(),
        bestPickupLeg + 1,
        bestDropoffLeg + 2,
        routeWith(bestPickupLeg, bestDropoffLeg, pickupTimes, dropoffTimes, pickupToDropoff),
        stopDuration,
        transitStop,
        snap.walkToPickup(),
        snap.walkFromDropoff()
      );
    }

    /**
     * The trip's segments with the passenger's pickup and dropoff in the given legs.
     *
     * @param pickupToDropoff the segment from the pickup to the dropoff, the passenger's own ride;
     *                        used, and non-null, only when both are in the same leg
     */
    private List<RoutedSegment> routeWith(
      int pickupLeg,
      int dropoffLeg,
      LegTimes pickupTimes,
      LegTimes dropoffTimes,
      @Nullable RoutedSegment pickupToDropoff
    ) {
      var segments = new ArrayList<RoutedSegment>(legCount + 2);
      for (int leg = 0; leg < legCount; leg++) {
        if (leg == pickupLeg && leg == dropoffLeg) {
          segments.add(Objects.requireNonNull(pickupTimes.fromLegStart(leg)));
          segments.add(Objects.requireNonNull(pickupToDropoff));
          segments.add(Objects.requireNonNull(dropoffTimes.toLegEnd(leg)));
        } else if (leg == pickupLeg) {
          segments.add(Objects.requireNonNull(pickupTimes.fromLegStart(leg)));
          segments.add(Objects.requireNonNull(pickupTimes.toLegEnd(leg)));
        } else if (leg == dropoffLeg) {
          segments.add(Objects.requireNonNull(dropoffTimes.fromLegStart(leg)));
          segments.add(Objects.requireNonNull(dropoffTimes.toLegEnd(leg)));
        } else {
          segments.add(baseline[leg]);
        }
      }
      return segments;
    }

    /** The driving times into and out of the legs for a point, kept for the rest of the trip. */
    private LegTimes legTimes(Vertex point) {
      return legTimesByVertex.computeIfAbsent(point, LegTimes::new);
    }

    /**
     * Per leg, the segment from the leg's start to a point and from the point to the leg's end,
     * each routed the first time it is asked for; {@code null} means unroutable.
     */
    private final class LegTimes {

      /** The pickup or dropoff point. */
      private final Vertex point;
      /** Per leg, the segment from the leg's start to the point, once asked for. */
      private final RoutedSegment[] fromLegStart = new RoutedSegment[legCount];
      /** Per leg, the segment from the point to the leg's end, once asked for. */
      private final RoutedSegment[] toLegEnd = new RoutedSegment[legCount];
      /** Per leg, whether {@code fromLegStart} was asked for, so a null is not routed again. */
      private final boolean[] fromLegStartRouted = new boolean[legCount];
      /** Per leg, whether {@code toLegEnd} was asked for, so a null is not routed again. */
      private final boolean[] toLegEndRouted = new boolean[legCount];

      LegTimes(Vertex point) {
        this.point = point;
      }

      /** The segment from the leg's start to the point. */
      @Nullable
      RoutedSegment fromLegStart(int leg) {
        if (!fromLegStartRouted[leg]) {
          fromLegStartRouted[leg] = true;
          fromLegStart[leg] = carpoolRouter.route(routableTrip.vertices().get(leg), point);
        }
        return fromLegStart[leg];
      }

      /** The segment from the point to the leg's end. */
      @Nullable
      RoutedSegment toLegEnd(int leg) {
        if (!toLegEndRouted[leg]) {
          toLegEndRouted[leg] = true;
          toLegEnd[leg] = carpoolRouter.route(point, routableTrip.vertices().get(leg + 1));
        }
        return toLegEnd[leg];
      }
    }
  }
}
