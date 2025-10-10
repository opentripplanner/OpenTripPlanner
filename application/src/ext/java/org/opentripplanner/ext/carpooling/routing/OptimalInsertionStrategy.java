package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.ext.carpooling.util.PassengerCountTimeline;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the optimal insertion positions for a passenger in a carpool trip.
 * <p>
 * Uses a brute-force approach to try all valid insertion combinations and
 * selects the one with minimum additional travel time. Delegates validation
 * to pluggable validators.
 * <p>
 * Algorithm:
 * 1. Build route points from the trip
 * 2. For each possible pickup position:
 *    - For each possible dropoff position after pickup:
 *      - Validate the insertion
 *      - Calculate route with actual A* routing
 *      - Track the best (minimum additional duration)
 * 3. Return the optimal candidate
 */
public class OptimalInsertionStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(OptimalInsertionStrategy.class);

  private final InsertionValidator validator;
  private final RoutingFunction routingFunction;
  private final PassengerDelayConstraints delayConstraints;
  private final BeelineEstimator beelineEstimator;

  public OptimalInsertionStrategy(InsertionValidator validator, RoutingFunction routingFunction) {
    this(validator, routingFunction, new PassengerDelayConstraints(), new BeelineEstimator());
  }

  public OptimalInsertionStrategy(
    InsertionValidator validator,
    RoutingFunction routingFunction,
    PassengerDelayConstraints delayConstraints
  ) {
    this(validator, routingFunction, delayConstraints, new BeelineEstimator());
  }

  public OptimalInsertionStrategy(
    InsertionValidator validator,
    RoutingFunction routingFunction,
    PassengerDelayConstraints delayConstraints,
    BeelineEstimator beelineEstimator
  ) {
    this.validator = validator;
    this.routingFunction = routingFunction;
    this.delayConstraints = delayConstraints;
    this.beelineEstimator = beelineEstimator;
  }

  /**
   * Finds the optimal insertion for a passenger in a trip.
   *
   * @param trip The carpool trip
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @return The optimal insertion candidate, or null if no valid insertion exists
   */
  public InsertionCandidate findOptimalInsertion(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    // Build route points and passenger timeline
    List<RoutePoint> routePoints = buildRoutePoints(trip);
    PassengerCountTimeline passengerTimeline = PassengerCountTimeline.build(trip);

    LOG.debug(
      "Evaluating insertion for trip {} with {} route points, {} capacity",
      trip.getId(),
      routePoints.size(),
      trip.availableSeats()
    );

    // Calculate baseline duration and cumulative times (current route without new passenger)
    Duration[] originalCumulativeTimes = calculateCumulativeTimes(routePoints);
    if (originalCumulativeTimes == null) {
      LOG.warn("Could not calculate baseline route for trip {}", trip.getId());
      return null;
    }
    Duration baselineDuration = originalCumulativeTimes[originalCumulativeTimes.length - 1];

    // Calculate beeline estimates for original route (for early rejection heuristic)
    List<WgsCoordinate> originalCoords = routePoints.stream().map(RoutePoint::coordinate).toList();
    Duration[] originalBeelineTimes = beelineEstimator.calculateCumulativeTimes(originalCoords);

    InsertionCandidate bestCandidate = null;
    Duration minAdditionalDuration = Duration.ofDays(1);

    // Try all valid insertion positions
    for (int pickupPos = 1; pickupPos <= routePoints.size(); pickupPos++) {
      for (int dropoffPos = pickupPos + 1; dropoffPos <= routePoints.size() + 1; dropoffPos++) {
        // Create validation context
        List<WgsCoordinate> routeCoords = routePoints.stream().map(RoutePoint::coordinate).toList();

        var validationContext = new InsertionValidator.ValidationContext(
          pickupPos,
          dropoffPos,
          passengerPickup,
          passengerDropoff,
          routeCoords,
          passengerTimeline
        );

        // Validate insertion
        var validationResult = validator.validate(validationContext);
        if (!validationResult.isValid()) {
          LOG.trace(
            "Insertion at pickup={}, dropoff={} rejected: {}",
            pickupPos,
            dropoffPos,
            validationResult.reason()
          );
          continue;
        }

        // Beeline delay heuristic check (early rejection before expensive A* routing)
        // Only check if there are existing stops to protect
        if (originalCoords.size() > 2) {
          if (
            !passesBeelineDelayCheck(
              originalCoords,
              originalBeelineTimes,
              passengerPickup,
              passengerDropoff,
              pickupPos,
              dropoffPos
            )
          ) {
            LOG.trace(
              "Insertion at pickup={}, dropoff={} rejected by beeline delay heuristic",
              pickupPos,
              dropoffPos
            );
            continue; // Skip expensive A* routing!
          }
        }

        // Calculate route with insertion
        InsertionCandidate candidate = evaluateInsertion(
          trip,
          routePoints,
          pickupPos,
          dropoffPos,
          passengerPickup,
          passengerDropoff,
          baselineDuration,
          originalCumulativeTimes
        );

        if (candidate != null) {
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
              pickupPos,
              dropoffPos,
              additionalDuration.getSeconds()
            );
          }
        }
      }
    }

    if (bestCandidate == null) {
      LOG.debug("No valid insertion found for trip {}", trip.getId());
    } else {
      LOG.info(
        "Optimal insertion for trip {}: pickup@{}, dropoff@{}, additional={}s",
        trip.getId(),
        bestCandidate.pickupPosition(),
        bestCandidate.dropoffPosition(),
        bestCandidate.additionalDuration().getSeconds()
      );
    }

    return bestCandidate;
  }

  /**
   * Evaluates a specific insertion configuration by routing all segments.
   */
  private InsertionCandidate evaluateInsertion(
    CarpoolTrip trip,
    List<RoutePoint> originalPoints,
    int pickupPos,
    int dropoffPos,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Duration baselineDuration,
    Duration[] originalCumulativeTimes
  ) {
    // Build modified route with passenger stops inserted
    List<RoutePoint> modifiedPoints = new ArrayList<>(originalPoints);
    modifiedPoints.add(pickupPos, new RoutePoint(passengerPickup, "Passenger-Pickup"));
    modifiedPoints.add(dropoffPos, new RoutePoint(passengerDropoff, "Passenger-Dropoff"));

    // Route all segments
    List<GraphPath<State, Edge, Vertex>> segments = new ArrayList<>();
    Duration totalDuration = Duration.ZERO;

    for (int i = 0; i < modifiedPoints.size() - 1; i++) {
      GenericLocation from = toGenericLocation(modifiedPoints.get(i).coordinate());
      GenericLocation to = toGenericLocation(modifiedPoints.get(i + 1).coordinate());

      GraphPath<State, Edge, Vertex> segment = routingFunction.route(from, to);
      if (segment == null) {
        LOG.trace("Routing failed for segment {} → {}", i, i + 1);
        return null; // This insertion is not viable
      }

      segments.add(segment);
      totalDuration = totalDuration.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    // Check passenger delay constraints
    if (
      !delayConstraints.satisfiesConstraints(
        originalCumulativeTimes,
        segments,
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
      segments,
      baselineDuration,
      totalDuration
    );
  }

  /**
   * Builds route points from a trip.
   */
  private List<RoutePoint> buildRoutePoints(CarpoolTrip trip) {
    List<RoutePoint> points = new ArrayList<>();

    // Boarding area
    points.add(new RoutePoint(trip.boardingArea().getCoordinate(), "Boarding-" + trip.getId()));

    // Existing stops
    for (CarpoolStop stop : trip.stops()) {
      points.add(new RoutePoint(stop.getCoordinate(), "Stop-" + stop.getSequenceNumber()));
    }

    // Alighting area
    points.add(new RoutePoint(trip.alightingArea().getCoordinate(), "Alighting-" + trip.getId()));

    return points;
  }

  /**
   * Calculates cumulative durations to each point in the route.
   * Returns an array where index i contains the cumulative duration to reach point i.
   *
   * @param routePoints The route points
   * @return Array of cumulative durations, or null if routing fails
   */
  private Duration[] calculateCumulativeTimes(List<RoutePoint> routePoints) {
    Duration[] cumulativeTimes = new Duration[routePoints.size()];
    cumulativeTimes[0] = Duration.ZERO;

    for (int i = 0; i < routePoints.size() - 1; i++) {
      GenericLocation from = toGenericLocation(routePoints.get(i).coordinate());
      GenericLocation to = toGenericLocation(routePoints.get(i + 1).coordinate());

      GraphPath<State, Edge, Vertex> segment = routingFunction.route(from, to);
      if (segment == null) {
        return null;
      }

      Duration segmentDuration = Duration.between(
        segment.states.getFirst().getTime(),
        segment.states.getLast().getTime()
      );
      cumulativeTimes[i + 1] = cumulativeTimes[i].plus(segmentDuration);
    }

    return cumulativeTimes;
  }

  private GenericLocation toGenericLocation(WgsCoordinate coord) {
    return GenericLocation.fromCoordinate(coord.latitude(), coord.longitude());
  }

  /**
   * Checks if an insertion position passes the beeline delay heuristic.
   * This is a fast, optimistic check using straight-line distance estimates.
   * If this check fails, we know the actual A* routing will also fail, so we
   * can skip the expensive routing calculation.
   *
   * @param originalCoords Original route coordinates
   * @param originalBeelineTimes Beeline cumulative times for original route
   * @param passengerPickup Passenger pickup location
   * @param passengerDropoff Passenger dropoff location
   * @param pickupPos Pickup insertion position (1-indexed)
   * @param dropoffPos Dropoff insertion position (1-indexed)
   * @return true if insertion might satisfy delay constraints (proceed with A* routing)
   */
  private boolean passesBeelineDelayCheck(
    List<WgsCoordinate> originalCoords,
    Duration[] originalBeelineTimes,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    int pickupPos,
    int dropoffPos
  ) {
    // Build modified coordinate list with passenger stops inserted
    List<WgsCoordinate> modifiedCoords = new ArrayList<>(originalCoords);
    modifiedCoords.add(pickupPos, passengerPickup);
    modifiedCoords.add(dropoffPos, passengerDropoff);

    // Calculate beeline times for modified route
    Duration[] modifiedBeelineTimes = beelineEstimator.calculateCumulativeTimes(modifiedCoords);

    // Check delays at each existing stop (exclude boarding at 0 and alighting at end)
    for (int originalIndex = 1; originalIndex < originalCoords.size() - 1; originalIndex++) {
      int modifiedIndex = getModifiedIndex(originalIndex, pickupPos, dropoffPos);

      Duration originalTime = originalBeelineTimes[originalIndex];
      Duration modifiedTime = modifiedBeelineTimes[modifiedIndex];
      Duration beelineDelay = modifiedTime.minus(originalTime);

      // If even the optimistic beeline estimate exceeds threshold, actual routing will too
      if (beelineDelay.compareTo(delayConstraints.getMaxDelay()) > 0) {
        LOG.trace(
          "Stop at position {} has beeline delay {}s (exceeds {}s threshold)",
          originalIndex,
          beelineDelay.getSeconds(),
          delayConstraints.getMaxDelay().getSeconds()
        );
        return false; // Reject early!
      }
    }

    return true; // Passes beeline check, proceed with A* routing
  }

  /**
   * Maps an index in the original route to the corresponding index in the
   * modified route after passenger stops have been inserted.
   *
   * @param originalIndex Index in original route
   * @param pickupPos Position where pickup was inserted (1-indexed)
   * @param dropoffPos Position where dropoff was inserted (1-indexed)
   * @return Corresponding index in modified route
   */
  private int getModifiedIndex(int originalIndex, int pickupPos, int dropoffPos) {
    int modifiedIndex = originalIndex;

    // Account for pickup insertion
    if (originalIndex >= pickupPos) {
      modifiedIndex++;
    }

    // Account for dropoff insertion (after pickup has been inserted)
    if (modifiedIndex >= dropoffPos) {
      modifiedIndex++;
    }

    return modifiedIndex;
  }

  /**
   * Functional interface for street routing.
   */
  @FunctionalInterface
  public interface RoutingFunction {
    GraphPath<State, Edge, Vertex> route(GenericLocation from, GenericLocation to);
  }
}
