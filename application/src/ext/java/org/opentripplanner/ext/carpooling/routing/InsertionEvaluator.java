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
 * pickup/dropoff and reusing the trip's baseline legs elsewhere.
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
    var baselineSegments = routeBaseline(routedTrip);
    if (baselineSegments == null) {
      return List.of();
    }
    return tripWithViableAccessEgress
      .viableAccessEgress()
      .stream()
      .map(viableAccessEgress ->
        selectBestPosition(
          routedTrip,
          viableAccessEgress.insertionPositions(),
          toPassengerSnap(viableAccessEgress),
          baselineSegments,
          viableAccessEgress.transitStop()
        )
      )
      .filter(Objects::nonNull)
      .toList();
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
    var baselineSegments = routeBaseline(routedTrip);
    if (baselineSegments == null) {
      return null;
    }
    return selectBestPosition(routedTrip, viablePositions, snap, baselineSegments, null);
  }

  @Nullable
  private GraphPath<State, Edge, Vertex>[] routeBaseline(RoutedCarpoolTrip routedTrip) {
    var baselineSegments = router.routeLegs(routedTrip.vertices());
    if (baselineSegments == null) {
      LOG.warn("Could not route baseline for trip {}", routedTrip.trip().getId());
    }
    return baselineSegments;
  }

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

  /** The valid position with the smallest total trip duration. */
  @Nullable
  private InsertionCandidate selectBestPosition(
    RoutedCarpoolTrip routedTrip,
    List<InsertionPosition> viablePositions,
    PassengerSnap snap,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    @Nullable NearbyStop transitStop
  ) {
    Duration[] baselineCumulative = routedTrip.cumulativeArrivals(stopDuration);
    InsertionCandidate best = null;
    for (InsertionPosition position : viablePositions) {
      var candidate = evaluatePosition(
        routedTrip,
        position,
        snap,
        baselineSegments,
        baselineCumulative,
        transitStop
      );
      if (candidate == null) {
        continue;
      }
      if (best == null || candidate.totalTripDuration().compareTo(best.totalTripDuration()) < 0) {
        best = candidate;
        LOG.debug(
          "New best insertion: pickup@{}, dropoff@{}, duration={}s",
          position.pickupPos(),
          position.dropoffPos(),
          candidate.totalTripDuration().getSeconds()
        );
      }
    }
    return best;
  }

  /**
   * Routes the detour segments around the inserted pickup/dropoff and reuses the baseline legs
   * elsewhere. {@code null} if a detour cannot be routed or the delay constraints are exceeded.
   */
  @Nullable
  private InsertionCandidate evaluatePosition(
    RoutedCarpoolTrip routedTrip,
    InsertionPosition position,
    PassengerSnap snap,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    Duration[] baselineCumulative,
    @Nullable NearbyStop transitStop
  ) {
    List<Vertex> modifiedPoints = new ArrayList<>(routedTrip.vertices());
    modifiedPoints.add(position.pickupPos(), snap.pickupVertex());
    modifiedPoints.add(position.dropoffPos(), snap.dropoffVertex());

    List<GraphPath<State, Edge, Vertex>> modifiedSegments = new ArrayList<>();
    Duration[] modifiedDurations = new Duration[modifiedPoints.size() - 1];
    for (int i = 0; i < modifiedDurations.length; i++) {
      int baselineIndex = position.baselineSegmentIndex(i);
      GraphPath<State, Edge, Vertex> segment;
      if (baselineIndex >= 0) {
        segment = baselineSegments[baselineIndex];
      } else {
        segment = router.route(modifiedPoints.get(i), modifiedPoints.get(i + 1));
        if (segment == null) {
          LOG.trace("Routing failed for new segment {} → {}", i, i + 1);
          return null;
        }
      }
      modifiedSegments.add(segment);
      modifiedDurations[i] = durationOrZero(segment);
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

    return new InsertionCandidate(
      routedTrip.trip(),
      position.pickupPos(),
      position.dropoffPos(),
      modifiedSegments,
      stopDuration,
      transitStop,
      snap.walkToPickup(),
      snap.walkFromDropoff()
    );
  }
}
