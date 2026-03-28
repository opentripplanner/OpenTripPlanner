package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateCumulativeDurations;
import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateDuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
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

  private static final Duration INITIAL_ADDITIONAL_DURATION = Duration.ofDays(1);

  private final PassengerDelayConstraints delayConstraints;
  private final LinkingContext linkingContext;
  private final StreetVertexUtils streetVertexUtils;
  private final CarpoolRouter carpoolRouter;

  /**
   * Creates an evaluator with the specified routing function, delay constraints, and linking context.
   *
   * @param delayConstraints Constraints for acceptable passenger delays
   * @param linkingContext Linking context with pre-linked vertices for routing
   */
  public InsertionEvaluator(
    PassengerDelayConstraints delayConstraints,
    LinkingContext linkingContext,
    StreetVertexUtils streetVertexUtils,
    CarpoolRouter carpoolRouter
  ) {
    this.delayConstraints = delayConstraints;
    this.linkingContext = linkingContext;
    this.streetVertexUtils = streetVertexUtils;
    this.carpoolRouter = carpoolRouter;
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

    GraphPath<State, Edge, Vertex>[] baselineSegments = routeSegments(tripWithVertices.vertices());
    if (baselineSegments == null) {
      LOG.error("Could not route baseline segments for trip {}", tripWithVertices.trip().getId());
      return List.of();
    }

    Duration[] cumulativeDurations = calculateCumulativeDurations(baselineSegments);

    GraphPath<State, Edge, Vertex> pathBetweenOriginAndDestination = carpoolRouter.route(
      tripWithVertices.vertices().getFirst(),
      tripWithVertices.vertices().getLast()
    );
    if (pathBetweenOriginAndDestination == null) {
      LOG.error(
        "Could not route between origin and destination for trip {}",
        tripWithVertices.trip().getId()
      );
      return List.of();
    }

    Duration durationBetweenOriginAndDestination = calculateDuration(
      pathBetweenOriginAndDestination
    );

    return tripWithViableAccessEgress
      .viableAccessEgress()
      .stream()
      .map(viableAccessEgress -> {
        var pickUpVertex = viableAccessEgress.accessEgress() == AccessEgressType.ACCESS
          ? viableAccessEgress.passengerVertex()
          : viableAccessEgress.transitVertex();
        var dropOffVertex = viableAccessEgress.accessEgress() == AccessEgressType.ACCESS
          ? viableAccessEgress.transitVertex()
          : viableAccessEgress.passengerVertex();

        return findBestInsertion(
          tripWithVertices,
          viableAccessEgress.insertionPositions(),
          pickUpVertex,
          dropOffVertex,
          baselineSegments,
          cumulativeDurations,
          durationBetweenOriginAndDestination,
          viableAccessEgress.transitStop()
        );
      })
      .filter(Objects::nonNull)
      .toList();
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
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @return The best insertion candidate, or null if none are viable after routing
   */
  @Nullable
  public InsertionCandidate findBestInsertion(
    CarpoolTripWithVertices tripWithVertices,
    List<InsertionPosition> viablePositions,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    GraphPath<State, Edge, Vertex>[] baselineSegments = routeSegments(tripWithVertices.vertices());
    if (baselineSegments == null) {
      LOG.warn("Could not route baseline for trip {}", tripWithVertices.trip().getId());
      return null;
    }

    var passengerPickupVertex = streetVertexUtils.getOrCreateVertex(
      passengerPickup,
      linkingContext
    );
    var passengerDropoffVertex = streetVertexUtils.getOrCreateVertex(
      passengerDropoff,
      linkingContext
    );

    Duration[] cumulativeDurations = calculateCumulativeDurations(baselineSegments);

    GraphPath<State, Edge, Vertex> pathBetweenOriginAndDestination = carpoolRouter.route(
      tripWithVertices.vertices().getFirst(),
      tripWithVertices.vertices().getLast()
    );
    if (pathBetweenOriginAndDestination == null) {
      LOG.error(
        "Could not route between origin and destination for trip {}",
        tripWithVertices.trip().getId()
      );
      return null;
    }

    Duration durationBetweenOriginAndDestination = calculateDuration(
      pathBetweenOriginAndDestination
    );

    return findBestInsertion(
      tripWithVertices,
      viablePositions,
      passengerPickupVertex,
      passengerDropoffVertex,
      baselineSegments,
      cumulativeDurations,
      durationBetweenOriginAndDestination,
      null
    );
  }

  @Nullable
  private InsertionCandidate findBestInsertion(
    CarpoolTripWithVertices tripWithVertices,
    List<InsertionPosition> viablePositions,
    Vertex passengerPickup,
    Vertex passengerDropoff,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    Duration[] cumulativeDurations,
    Duration durationBetweenOriginAndDestination,
    NearbyStop transitStop
  ) {
    InsertionCandidate bestCandidate = null;
    Duration minAdditionalDuration = INITIAL_ADDITIONAL_DURATION;

    for (InsertionPosition position : viablePositions) {
      InsertionCandidate candidate = evaluateInsertion(
        tripWithVertices,
        position.pickupPos(),
        position.dropoffPos(),
        passengerPickup,
        passengerDropoff,
        baselineSegments,
        cumulativeDurations,
        durationBetweenOriginAndDestination,
        transitStop
      );

      if (candidate == null) {
        continue;
      }

      Duration additionalDuration = candidate.additionalDuration();

      // Check if this is the best so far and within deviation budget
      if (
        additionalDuration.compareTo(minAdditionalDuration) < 0 &&
        additionalDuration.compareTo(tripWithVertices.trip().deviationBudget()) <= 0
      ) {
        minAdditionalDuration = additionalDuration;
        bestCandidate = candidate;
        LOG.debug(
          "New best insertion: pickup@{}, dropoff@{}, additional={}s",
          position.pickupPos(),
          position.dropoffPos(),
          additionalDuration.getSeconds()
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
    Vertex passengerPickup,
    Vertex passengerDropoff,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    Duration[] originalCumulativeDurations,
    Duration durationBetweenOriginAndDestination,
    NearbyStop transitStop
  ) {
    List<GraphPath<State, Edge, Vertex>> modifiedSegments = buildModifiedSegments(
      tripWithVertices.vertices(),
      baselineSegments,
      pickupPos,
      dropoffPos,
      passengerPickup,
      passengerDropoff
    );

    if (modifiedSegments == null) {
      return null;
    }

    // Calculate total duration
    Duration totalDuration = Duration.ZERO;
    for (GraphPath<State, Edge, Vertex> segment : modifiedSegments) {
      totalDuration = totalDuration.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    // Check passenger delay constraints
    if (
      !delayConstraints.satisfiesConstraints(
        originalCumulativeDurations,
        calculateCumulativeDurations(
          modifiedSegments.toArray(new GraphPath[modifiedSegments.size()])
        ),
        pickupPos,
        dropoffPos
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
      durationBetweenOriginAndDestination,
      totalDuration,
      transitStop
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

    // Build modified point list
    List<Vertex> modifiedPoints = new ArrayList<>(originalPoints);
    modifiedPoints.add(pickupPos, passengerPickup);
    modifiedPoints.add(dropoffPos, passengerDropoff);

    // For each segment in the modified route:
    // - Reuse baseline segment if it didn't change
    // - Route new segment if it involves passenger stops
    for (int i = 0; i < modifiedPoints.size() - 1; i++) {
      GraphPath<State, Edge, Vertex> segment;

      // Check if this segment can be reused from baseline
      int baselineIndex = getBaselineSegmentIndex(i, originalPoints, modifiedPoints);
      if (baselineIndex >= 0 && baselineIndex < baselineSegments.length) {
        // This segment is unchanged - reuse it!
        segment = baselineSegments[baselineIndex];
        LOG.trace("Reusing baseline segment {} for modified position {}", baselineIndex, i);
      } else {
        // This segment involves passenger - route it
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
   * Maps a modified route segment index to the corresponding baseline segment index.
   * Returns -1 if the segment cannot be reused (endpoints don't match).
   *
   * <p>A baseline segment can only be reused if BOTH endpoints match exactly between
   * the baseline and modified routes. This ensures we don't reuse a segment whose
   * endpoints have changed due to passenger insertion.
   *
   * @param modifiedIndex Index in modified route (with passenger inserted)
   * @param originalPoints Original route points (before passenger insertion)
   * @param modifiedPoints Modified route points (after passenger insertion)
   * @return Baseline segment index if endpoints match, or -1 if segment must be routed
   */
  private int getBaselineSegmentIndex(
    int modifiedIndex,
    List<Vertex> originalPoints,
    List<Vertex> modifiedPoints
  ) {
    // Get the start and end coordinates of this modified segment
    Vertex modifiedStart = modifiedPoints.get(modifiedIndex);
    Vertex modifiedEnd = modifiedPoints.get(modifiedIndex + 1);

    // Search through baseline segments to find one with matching endpoints
    for (int baselineIndex = 0; baselineIndex < originalPoints.size() - 1; baselineIndex++) {
      Vertex baselineStart = originalPoints.get(baselineIndex);
      Vertex baselineEnd = originalPoints.get(baselineIndex + 1);

      // Check if both endpoints match (using Vertex's built-in equality)
      if (modifiedStart.equals(baselineStart) && modifiedEnd.equals(baselineEnd)) {
        LOG.trace(
          "Modified segment {} matches baseline segment {} (endpoints match)",
          modifiedIndex,
          baselineIndex
        );
        return baselineIndex;
      }
    }

    LOG.trace(
      "Modified segment {} has no matching baseline segment (endpoints changed)",
      modifiedIndex
    );
    return -1;
  }
}
