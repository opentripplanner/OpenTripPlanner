package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateCumulativeDurations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.linking.LinkingContext;
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

  private final RoutingFunction routingFunction;
  private final PassengerDelayConstraints delayConstraints;
  private final LinkingContext linkingContext;

  /**
   * Creates an evaluator with the specified routing function, delay constraints, and linking context.
   *
   * @param routingFunction Function that performs A* routing between coordinates
   * @param delayConstraints Constraints for acceptable passenger delays
   * @param linkingContext Linking context with pre-linked vertices for routing
   */
  public InsertionEvaluator(
    RoutingFunction routingFunction,
    PassengerDelayConstraints delayConstraints,
    LinkingContext linkingContext
  ) {
    this.routingFunction = routingFunction;
    this.delayConstraints = delayConstraints;
    this.linkingContext = linkingContext;
  }

  /**
   * Routes all baseline segments and caches the results.
   *
   * @return Array of routed segments, or null if any segment fails to route
   */
  @SuppressWarnings("unchecked")
  private GraphPath<State, Edge, Vertex>[] routeBaselineSegments(List<WgsCoordinate> routePoints) {
    GraphPath<State, Edge, Vertex>[] segments = new GraphPath[routePoints.size() - 1];

    for (int i = 0; i < routePoints.size() - 1; i++) {
      var fromCoord = routePoints.get(i);
      var toCoord = routePoints.get(i + 1);
      GenericLocation from = GenericLocation.fromCoordinate(
        fromCoord.latitude(),
        fromCoord.longitude()
      );
      GenericLocation to = GenericLocation.fromCoordinate(toCoord.latitude(), toCoord.longitude());

      GraphPath<State, Edge, Vertex> segment = routingFunction.route(from, to, linkingContext);
      if (segment == null) {
        LOG.debug("Baseline routing failed for segment {} → {}", i, i + 1);
        return null;
      }

      segments[i] = segment;
    }

    return segments;
  }

  /**
   * Evaluates pre-filtered insertion positions using A* routing.
   * <p>
   * This method assumes the provided positions have already passed heuristic
   * validation (capacity, direction, beeline delay). It performs expensive
   * A* routing for each position and selects the one with minimum additional
   * duration that satisfies delay constraints.
   *
   * @param trip The carpool trip
   * @param viablePositions Positions that passed heuristic checks (from InsertionPositionFinder)
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @return The best insertion candidate, or null if none are viable after routing
   */
  @Nullable
  public InsertionCandidate findBestInsertion(
    CarpoolTrip trip,
    List<InsertionPosition> viablePositions,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    GraphPath<State, Edge, Vertex>[] baselineSegments = routeBaselineSegments(trip.routePoints());
    if (baselineSegments == null) {
      LOG.warn("Could not route baseline for trip {}", trip.getId());
      return null;
    }

    Duration[] cumulativeDurations = calculateCumulativeDurations(baselineSegments);

    InsertionCandidate bestCandidate = null;
    Duration minAdditionalDuration = INITIAL_ADDITIONAL_DURATION;
    Duration baselineDuration = cumulativeDurations[cumulativeDurations.length - 1];

    for (InsertionPosition position : viablePositions) {
      InsertionCandidate candidate = evaluateInsertion(
        trip,
        position.pickupPos(),
        position.dropoffPos(),
        passengerPickup,
        passengerDropoff,
        baselineSegments,
        cumulativeDurations,
        baselineDuration
      );

      if (candidate == null) {
        continue;
      }

      Duration additionalDuration = candidate.additionalDuration();

      // Check if this is the best so far and within deviation budget
      if (
        additionalDuration.compareTo(minAdditionalDuration) < 0 &&
        additionalDuration.compareTo(trip.deviationBudget()) <= 0
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
    CarpoolTrip trip,
    int pickupPos,
    int dropoffPos,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    Duration[] originalCumulativeDurations,
    Duration baselineDuration
  ) {
    // Build modified route segments by reusing cached baseline segments
    List<GraphPath<State, Edge, Vertex>> modifiedSegments = buildModifiedSegments(
      trip.routePoints(),
      baselineSegments,
      pickupPos,
      dropoffPos,
      passengerPickup,
      passengerDropoff
    );

    if (modifiedSegments == null) {
      // Routing failed for new segments
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
      trip,
      pickupPos,
      dropoffPos,
      modifiedSegments,
      baselineDuration,
      totalDuration
    );
  }

  /**
   * Builds modified route segments by reusing cached baseline segments where possible
   * and only routing new segments that involve the passenger.
   *
   * <p>This is the key optimization: instead of routing ALL segments again,
   * we only route segments that changed due to passenger insertion.
   *
   * @param originalPoints Route points before passenger insertion
   * @param baselineSegments Pre-routed segments for baseline route
   * @param pickupPos Passenger pickup position (1-indexed)
   * @param dropoffPos Passenger dropoff position (1-indexed)
   * @param passengerPickup Passenger's pickup coordinate
   * @param passengerDropoff Passenger's dropoff coordinate
   * @return List of segments for modified route, or null if routing fails
   */
  private List<GraphPath<State, Edge, Vertex>> buildModifiedSegments(
    List<WgsCoordinate> originalPoints,
    GraphPath<State, Edge, Vertex>[] baselineSegments,
    int pickupPos,
    int dropoffPos,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    List<GraphPath<State, Edge, Vertex>> segments = new ArrayList<>();

    // Build modified point list
    List<WgsCoordinate> modifiedPoints = new ArrayList<>(originalPoints);
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
        var fromCoord = modifiedPoints.get(i);
        var toCoord = modifiedPoints.get(i + 1);
        GenericLocation from = GenericLocation.fromCoordinate(
          fromCoord.latitude(),
          fromCoord.longitude()
        );
        GenericLocation to = GenericLocation.fromCoordinate(
          toCoord.latitude(),
          toCoord.longitude()
        );

        segment = routingFunction.route(from, to, linkingContext);
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
    List<WgsCoordinate> originalPoints,
    List<WgsCoordinate> modifiedPoints
  ) {
    // Get the start and end coordinates of this modified segment
    WgsCoordinate modifiedStart = modifiedPoints.get(modifiedIndex);
    WgsCoordinate modifiedEnd = modifiedPoints.get(modifiedIndex + 1);

    // Search through baseline segments to find one with matching endpoints
    for (int baselineIndex = 0; baselineIndex < originalPoints.size() - 1; baselineIndex++) {
      WgsCoordinate baselineStart = originalPoints.get(baselineIndex);
      WgsCoordinate baselineEnd = originalPoints.get(baselineIndex + 1);

      // Check if both endpoints match (using WgsCoordinate's built-in equality)
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
