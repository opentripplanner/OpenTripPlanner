package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateCumulativeDurations;
import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.durationOrZero;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects the best insertion among pre-screened positions, routing the detour around each candidate
 * pickup/dropoff and the baseline legs the winner reuses.
 */
public class InsertionEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(InsertionEvaluator.class);

  private final CarpoolRouter router;
  private final Duration stopDuration;

  /**
   * @param stopDuration dwell time at each intermediate stop, from the car {@code pickupTime}
   *        preference.
   */
  public InsertionEvaluator(CarpoolRouter router, Duration stopDuration) {
    this.router = router;
    this.stopDuration = stopDuration;
  }

  /** One candidate per nearby stop that has a valid insertion. */
  public List<InsertionCandidate> findBestInsertions(
    TripWithViableAccessEgress tripWithViableAccessEgress
  ) {
    var routedTrip = tripWithViableAccessEgress.routedTrip();

    List<StopInsertion> winners = tripWithViableAccessEgress
      .viableAccessEgress()
      .stream()
      .map(viableAccessEgress -> {
        var snap = toPassengerSnap(viableAccessEgress);
        var best = selectBestPosition(routedTrip, viableAccessEgress.insertionPositions(), snap);
        return best == null
          ? null
          : new StopInsertion(snap, best, viableAccessEgress.transitStop());
      })
      .filter(Objects::nonNull)
      .toList();

    return materializeWinners(routedTrip, winners);
  }

  /** A chosen insertion awaiting the baseline legs it reuses. */
  private record StopInsertion(
    PassengerSnap snap,
    SelectedPosition selected,
    @Nullable NearbyStop transitStop
  ) {}

  private static PassengerSnap toPassengerSnap(ViableAccessEgress viableAccessEgress) {
    boolean isAccess = viableAccessEgress.accessEgress() == AccessEgressType.ACCESS;
    var pickup = isAccess
      ? viableAccessEgress.passengerVertex()
      : viableAccessEgress.transitVertex();
    var dropoff = isAccess
      ? viableAccessEgress.transitVertex()
      : viableAccessEgress.passengerVertex();
    return new PassengerSnap(
      pickup,
      dropoff,
      viableAccessEgress.walkToPickup(),
      viableAccessEgress.walkFromDropoff()
    );
  }

  /**
   * @param snap pickup/dropoff vertices, already snapped, plus the walk paths bracketing the ride.
   */
  @Nullable
  public InsertionCandidate findBestInsertion(
    RoutedCarpoolTrip routedTrip,
    List<InsertionPosition> viablePositions,
    PassengerSnap snap
  ) {
    var best = selectBestPosition(routedTrip, viablePositions, snap);
    if (best == null) {
      return null;
    }
    var candidates = materializeWinners(routedTrip, List.of(new StopInsertion(snap, best, null)));
    return candidates.isEmpty() ? null : candidates.getFirst();
  }

  /** Routes the baseline legs the winners reuse and stitches each winner's geometry together. */
  private List<InsertionCandidate> materializeWinners(
    RoutedCarpoolTrip routedTrip,
    List<StopInsertion> winners
  ) {
    if (winners.isEmpty()) {
      return List.of();
    }
    var baselineLegs = routeReusedBaselineLegs(routedTrip, winners);
    if (baselineLegs == null) {
      return List.of();
    }
    return winners
      .stream()
      .map(winner ->
        materialize(
          routedTrip,
          winner.selected(),
          winner.snap(),
          baselineLegs,
          winner.transitStop()
        )
      )
      .toList();
  }

  /**
   * Routes only the baseline legs some winner reuses, leaving the rest {@code null}. An insertion
   * replaces the legs around its pickup and dropoff, so a short trip reuses none at all.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  private GraphPath<State, Edge, Vertex>[] routeReusedBaselineLegs(
    RoutedCarpoolTrip routedTrip,
    List<StopInsertion> winners
  ) {
    var vertices = routedTrip.vertices();
    GraphPath<State, Edge, Vertex>[] legs = new GraphPath[vertices.size() - 1];
    var reused = new boolean[legs.length];
    for (var winner : winners) {
      var detours = winner.selected().detourSegments();
      for (int i = 0; i < detours.length; i++) {
        if (detours[i] == null) {
          reused[winner.selected().position().baselineSegmentIndex(i)] = true;
        }
      }
    }
    for (int leg = 0; leg < legs.length; leg++) {
      if (!reused[leg]) {
        continue;
      }
      var path = router.route(vertices.get(leg), vertices.get(leg + 1));
      if (path == null) {
        LOG.warn("Could not route baseline leg {} of trip {}", leg, routedTrip.trip().getId());
        return null;
      }
      legs[leg] = path;
    }
    return legs;
  }

  /** The valid position with the smallest total trip duration. Only detours are routed. */
  @Nullable
  private SelectedPosition selectBestPosition(
    RoutedCarpoolTrip routedTrip,
    List<InsertionPosition> viablePositions,
    PassengerSnap snap
  ) {
    Duration[] baselineCumulative = routedTrip.cumulativeArrivals(stopDuration);
    SelectedPosition best = null;
    for (InsertionPosition position : viablePositions) {
      var evaluated = evaluatePosition(routedTrip, position, snap, baselineCumulative);
      if (evaluated == null) {
        continue;
      }
      if (best == null || evaluated.totalTripDuration().compareTo(best.totalTripDuration()) < 0) {
        best = evaluated;
        LOG.debug(
          "New best insertion: pickup@{}, dropoff@{}, duration={}s",
          position.pickupPos(),
          position.dropoffPos(),
          evaluated.totalTripDuration().getSeconds()
        );
      }
    }
    return best;
  }

  /** {@code null} if a detour cannot be routed or the delay constraints are exceeded. */
  @Nullable
  @SuppressWarnings("unchecked")
  private SelectedPosition evaluatePosition(
    RoutedCarpoolTrip routedTrip,
    InsertionPosition position,
    PassengerSnap snap,
    Duration[] baselineCumulative
  ) {
    List<Vertex> modifiedPoints = new ArrayList<>(routedTrip.vertices());
    modifiedPoints.add(position.pickupPos(), snap.pickupVertex());
    modifiedPoints.add(position.dropoffPos(), snap.dropoffVertex());

    // Detour segments are routed and kept; reused legs stay null and are filled in by materialize().
    GraphPath<State, Edge, Vertex>[] detourSegments = new GraphPath[modifiedPoints.size() - 1];
    Duration[] modifiedDurations = new Duration[detourSegments.length];
    for (int i = 0; i < detourSegments.length; i++) {
      int baselineIndex = position.baselineSegmentIndex(i);
      if (baselineIndex >= 0) {
        modifiedDurations[i] = routedTrip.legDurations()[baselineIndex];
      } else {
        var segment = router.route(modifiedPoints.get(i), modifiedPoints.get(i + 1));
        if (segment == null) {
          LOG.trace("Routing failed for new segment {} → {}", i, i + 1);
          return null;
        }
        detourSegments[i] = segment;
        modifiedDurations[i] = durationOrZero(segment);
      }
    }

    Duration[] modifiedCumulative = calculateCumulativeDurations(modifiedDurations, stopDuration);
    if (
      !PassengerDelayConstraints.satisfiesConstraints(
        baselineCumulative,
        modifiedCumulative,
        position.pickupPos(),
        position.dropoffPos(),
        routedTrip.trip().stops()
      )
    ) {
      LOG.trace(
        "Insertion at pickup={}, dropoff={} rejected by delay constraints",
        position.pickupPos(),
        position.dropoffPos()
      );
      return null;
    }

    return new SelectedPosition(
      position,
      detourSegments,
      modifiedCumulative[modifiedCumulative.length - 1]
    );
  }

  /** Stitches the routed baseline legs into the detour segments to build the final candidate. */
  private InsertionCandidate materialize(
    RoutedCarpoolTrip routedTrip,
    SelectedPosition selected,
    PassengerSnap snap,
    GraphPath<State, Edge, Vertex>[] baselineLegs,
    @Nullable NearbyStop transitStop
  ) {
    var detourSegments = selected.detourSegments();
    List<GraphPath<State, Edge, Vertex>> modifiedSegments = new ArrayList<>(detourSegments.length);
    for (int i = 0; i < detourSegments.length; i++) {
      modifiedSegments.add(
        detourSegments[i] != null
          ? detourSegments[i]
          : baselineLegs[selected.position().baselineSegmentIndex(i)]
      );
    }

    return new InsertionCandidate(
      routedTrip.trip(),
      selected.position().pickupPos(),
      selected.position().dropoffPos(),
      modifiedSegments,
      stopDuration,
      transitStop,
      snap.walkToPickup(),
      snap.walkFromDropoff()
    );
  }

  /** Detour segments carry a {@code null} in every slot that reuses a baseline leg. */
  private record SelectedPosition(
    InsertionPosition position,
    GraphPath<State, Edge, Vertex>[] detourSegments,
    Duration totalTripDuration
  ) {}
}
