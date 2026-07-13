package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateCumulativeDurations;

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
 * Evaluates pre-filtered insertion positions using A* routing.
 * <p>
 * This class is a pure evaluator that takes positions identified by heuristic
 * filtering and evaluates them using expensive A* street routing. It selects
 * the insertion that minimizes additional travel time while satisfying
 * passenger delay constraints.
 * <p>
 * This follows the established OTP pattern of separating candidate generation
 * from evaluation, similar to {@code TransferGenerator} and {@code OptimizePathDomainService}.
 */
public class InsertionEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(InsertionEvaluator.class);

  private final CarpoolRouter carpoolRouter;

  @Nullable
  private final CarpoolRouter baselineFallbackRouter;

  private final Duration stopDuration;

  /**
   * @param carpoolRouter routes a single street segment between two vertices in the candidate
   *        carpool route — used both to build the baseline route and to re-route only the
   *        segments that change when a passenger pickup/dropoff is inserted.
   * @param stopDuration duration added at each intermediate stop (from the car {@code pickupTime}
   *        preference); applied between consecutive segments when computing total trip and
   *        passenger-ride durations.
   */
  public InsertionEvaluator(CarpoolRouter carpoolRouter, Duration stopDuration) {
    this(carpoolRouter, null, stopDuration);
  }

  /**
   * @param carpoolRouter routes a single street segment between two vertices in the candidate
   *        carpool route — used both to build the baseline route and to re-route only the
   *        segments that change when a passenger pickup/dropoff is inserted.
   * @param baselineFallbackRouter re-routes a baseline leg that {@code carpoolRouter} could not,
   *        or {@code null} for no fallback. Only the baseline route falls back. When
   *        {@code carpoolRouter} is a tree router whose per-leg trees are sized to a bounded
   *        per-leg limit, an underestimate of that limit would leave a waypoint outside its tree
   *        and drop the whole trip; a goal-directed fallback re-routes just that leg instead.
   *        Inserted passenger
   *        segments deliberately do not fall back — a segment that cannot be routed within the
   *        tree is an insertion the delay constraints reject anyway, so failing fast there is
   *        correct and avoids a goal-directed search per nearby stop.
   * @param stopDuration duration added at each intermediate stop (from the car {@code pickupTime}
   *        preference); applied between consecutive segments when computing total trip and
   *        passenger-ride durations.
   */
  public InsertionEvaluator(
    CarpoolRouter carpoolRouter,
    @Nullable CarpoolRouter baselineFallbackRouter,
    Duration stopDuration
  ) {
    this.carpoolRouter = carpoolRouter;
    this.baselineFallbackRouter = baselineFallbackRouter;
    this.stopDuration = stopDuration;
  }

  /**
   * Routes all segments of routePoints
   *
   * @return Array of routed segments, or null if any segment fails to route
   */
  @SuppressWarnings("unchecked")
  private GraphPath<State, Edge, Vertex>[] routeSegments(List<Vertex> routePoints) {
    GraphPath<State, Edge, Vertex>[] segments = new GraphPath[routePoints.size() - 1];

    for (int i = 0; i < routePoints.size() - 1; i++) {
      var from = routePoints.get(i);
      var to = routePoints.get(i + 1);

      GraphPath<State, Edge, Vertex> segment = carpoolRouter.route(from, to);
      if (segment == null && baselineFallbackRouter != null) {
        segment = baselineFallbackRouter.route(from, to);
        if (segment != null) {
          LOG.debug("Baseline segment {} → {} routed by the fallback router", i, i + 1);
        }
      }
      if (segment == null) {
        LOG.debug("Baseline routing failed for segment {} → {}", i, i + 1);
        return null;
      }

      segments[i] = segment;
    }

    return segments;
  }

  /**
   * @return A list containing the best insertion that can be found for every NearbyStop in
   * the list tripWithViableAccessEgress.viableAccessEgress. If there are no valid insertions
   * for a NearbyStop, then no candidate for that stop will be returned.
   */
  public List<InsertionCandidate> findBestInsertions(
    TripWithViableAccessEgress tripWithViableAccessEgress
  ) {
    var tripWithVertices = tripWithViableAccessEgress.tripWithVertices();

    // No nearby stop produced a viable insertion position for this trip, so there is nothing to
    // evaluate. Return before routing the baseline: that routing builds the trip's street trees,
    // which are expensive for long trips — wasted work for a trip that cannot yield an
    // access/egress leg.
    if (tripWithViableAccessEgress.viableAccessEgress().isEmpty()) {
      return List.of();
    }

    GraphPath<State, Edge, Vertex>[] baselineSegments = routeSegments(tripWithVertices.vertices());
    if (baselineSegments == null) {
      LOG.error("Could not route baseline segments for trip {}", tripWithVertices.trip().getId());
      return List.of();
    }

    Duration[] cumulativeDurations = calculateCumulativeDurations(baselineSegments, stopDuration);

    return tripWithViableAccessEgress
      .viableAccessEgress()
      .stream()
      .map(viableAccessEgress -> {
        var snap = toPassengerSnap(viableAccessEgress);
        return findBestInsertion(
          tripWithVertices,
          viableAccessEgress.insertionPositions(),
          snap,
          baselineSegments,
          cumulativeDurations,
          viableAccessEgress.transitStop()
        );
      })
      .filter(Objects::nonNull)
      .toList();
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

  /**
   * Evaluates pre-filtered insertion positions using A* routing.
   * <p>
   * This method assumes the provided positions have already passed heuristic
   * validation (capacity, direction, beeline delay). It performs expensive
   * A* routing for each position and selects the one with minimum additional
   * duration that satisfies delay constraints.
   *
   * @param tripWithVertices The carpool trip with resolved vertices
   * @param viablePositions Positions that passed heuristic checks (from InsertionPositionFinder)
   * @param snap Pickup/dropoff vertices (already snapped to car-accessible vertices by the
   *        caller) and the optional walk paths bracketing the carpool ride
   * @return The best insertion candidate, or null if none are viable after routing
   */
  @Nullable
  public InsertionCandidate findBestInsertion(
    CarpoolTripWithVertices tripWithVertices,
    List<InsertionPosition> viablePositions,
    PassengerSnap snap
  ) {
    GraphPath<State, Edge, Vertex>[] baselineSegments = routeSegments(tripWithVertices.vertices());
    if (baselineSegments == null) {
      LOG.warn("Could not route baseline for trip {}", tripWithVertices.trip().getId());
      return null;
    }

    Duration[] cumulativeDurations = calculateCumulativeDurations(baselineSegments, stopDuration);

    return findBestInsertion(
      tripWithVertices,
      viablePositions,
      snap,
      baselineSegments,
      cumulativeDurations,
      null
    );
  }

  @Nullable
  private InsertionCandidate findBestInsertion(
    CarpoolTripWithVertices tripWithVertices,
    List<InsertionPosition> viablePositions,
    PassengerSnap snap,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    Duration[] cumulativeDurations,
    NearbyStop transitStop
  ) {
    InsertionCandidate bestCandidate = null;

    for (InsertionPosition position : viablePositions) {
      InsertionCandidate candidate = evaluateInsertion(
        tripWithVertices,
        position.pickupPos(),
        position.dropoffPos(),
        snap,
        baselineSegments,
        cumulativeDurations,
        transitStop
      );

      if (candidate == null) {
        continue;
      }

      if (
        bestCandidate == null ||
        candidate.totalTripDuration().compareTo(bestCandidate.totalTripDuration()) < 0
      ) {
        bestCandidate = candidate;
        LOG.debug(
          "New best insertion: pickup@{}, dropoff@{}, duration={}s",
          position.pickupPos(),
          position.dropoffPos(),
          candidate.totalTripDuration().getSeconds()
        );
      }
    }

    return bestCandidate;
  }

  /**
   * Evaluates a specific insertion configuration.
   * Reuses cached baseline segments and only routes new segments involving the passenger.
   */
  private InsertionCandidate evaluateInsertion(
    CarpoolTripWithVertices tripWithVertices,
    int pickupPos,
    int dropoffPos,
    PassengerSnap snap,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    Duration[] originalCumulativeDurations,
    NearbyStop transitStop
  ) {
    List<GraphPath<State, Edge, Vertex>> modifiedSegments = buildModifiedSegments(
      tripWithVertices.vertices(),
      baselineSegments,
      pickupPos,
      dropoffPos,
      snap.pickupVertex(),
      snap.dropoffVertex()
    );

    if (modifiedSegments == null) {
      return null;
    }

    Duration[] modifiedCumulativeDurations = calculateCumulativeDurations(
      modifiedSegments.toArray(new GraphPath[modifiedSegments.size()]),
      stopDuration
    );
    if (
      !PassengerDelayConstraints.satisfiesConstraints(
        originalCumulativeDurations,
        modifiedCumulativeDurations,
        pickupPos,
        dropoffPos,
        tripWithVertices.trip().stops()
      )
    ) {
      LOG.trace(
        "Insertion at pickup={}, dropoff={} rejected by delay constraints",
        pickupPos,
        dropoffPos
      );
      return null;
    }

    return new InsertionCandidate(
      tripWithVertices.trip(),
      pickupPos,
      dropoffPos,
      modifiedSegments,
      stopDuration,
      transitStop,
      snap.walkToPickup(),
      snap.walkFromDropoff()
    );
  }

  private List<GraphPath<State, Edge, Vertex>> buildModifiedSegments(
    List<Vertex> originalPoints,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    int pickupPos,
    int dropoffPos,
    Vertex passengerPickup,
    Vertex passengerDropoff
  ) {
    List<GraphPath<State, Edge, Vertex>> segments = new ArrayList<>();

    List<Vertex> modifiedPoints = new ArrayList<>(originalPoints);
    modifiedPoints.add(pickupPos, passengerPickup);
    modifiedPoints.add(dropoffPos, passengerDropoff);

    for (int i = 0; i < modifiedPoints.size() - 1; i++) {
      GraphPath<State, Edge, Vertex> segment;

      int baselineIndex = baselineSegmentIndex(i, pickupPos, dropoffPos);
      if (baselineIndex >= 0 && baselineIndex < baselineSegments.length) {
        segment = baselineSegments[baselineIndex];
        LOG.trace("Reusing baseline segment {} for modified position {}", baselineIndex, i);
      } else {
        var fromVertex = modifiedPoints.get(i);
        var toVertex = modifiedPoints.get(i + 1);

        segment = this.carpoolRouter.route(fromVertex, toVertex);
        if (segment == null) {
          LOG.trace("Routing failed for new segment {} → {}", i, i + 1);
          return null;
        }
        LOG.trace("Routed new segment for modified position {}", i);
      }

      segments.add(segment);
    }

    return segments;
  }

  /**
   * Maps a modified-route segment index to the corresponding baseline segment index, or
   * {@code -1} if the modified segment touches the inserted pickup or dropoff and so cannot be
   * reused.
   * <p>
   * The modified route is the original route with two insertions: pickup at {@code pickupPos}
   * and dropoff at {@code dropoffPos} (List.add semantics, dropoffPos interpreted after the
   * pickup insertion). A modified segment {@code [i, i+1)} either reuses an original segment
   * (when both endpoints fall in original points) or is one of the four newly created segments
   * around the inserted points.
   * <p>
   * Requires {@code pickupPos < dropoffPos} — the shift arithmetic depends on the pickup being
   * strictly before the dropoff, otherwise the result would silently describe a route with the
   * dropoff before the pickup. Throws {@link IllegalArgumentException} if the precondition is
   * violated.
   */
  private static int baselineSegmentIndex(int modifiedIndex, int pickupPos, int dropoffPos) {
    if (pickupPos >= dropoffPos) {
      throw new IllegalArgumentException(
        "pickupPos (" + pickupPos + ") must be < dropoffPos (" + dropoffPos + ")"
      );
    }
    int i = modifiedIndex;
    if (i == pickupPos - 1 || i == pickupPos || i == dropoffPos - 1 || i == dropoffPos) {
      return -1;
    }
    if (i < pickupPos) {
      return i;
    }
    if (i < dropoffPos) {
      return i - 1;
    }
    return i - 2;
  }
}
