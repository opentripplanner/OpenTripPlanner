package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
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

  public OptimalInsertionStrategy(InsertionValidator validator, RoutingFunction routingFunction) {
    this.validator = validator;
    this.routingFunction = routingFunction;
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

    // Calculate baseline duration (current route without new passenger)
    Duration baselineDuration = calculateRouteDuration(routePoints);
    if (baselineDuration == null) {
      LOG.warn("Could not calculate baseline duration for trip {}", trip.getId());
      return null;
    }

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

        // Calculate route with insertion
        InsertionCandidate candidate = evaluateInsertion(
          trip,
          routePoints,
          pickupPos,
          dropoffPos,
          passengerPickup,
          passengerDropoff,
          baselineDuration
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
    Duration baselineDuration
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
   * Calculates the total duration for a route.
   */
  private Duration calculateRouteDuration(List<RoutePoint> routePoints) {
    Duration total = Duration.ZERO;

    for (int i = 0; i < routePoints.size() - 1; i++) {
      GenericLocation from = toGenericLocation(routePoints.get(i).coordinate());
      GenericLocation to = toGenericLocation(routePoints.get(i + 1).coordinate());

      GraphPath<State, Edge, Vertex> segment = routingFunction.route(from, to);
      if (segment == null) {
        return null;
      }

      total = total.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    return total;
  }

  private GenericLocation toGenericLocation(WgsCoordinate coord) {
    return GenericLocation.fromCoordinate(coord.latitude(), coord.longitude());
  }

  /**
   * Functional interface for street routing.
   */
  @FunctionalInterface
  public interface RoutingFunction {
    GraphPath<State, Edge, Vertex> route(GenericLocation from, GenericLocation to);
  }
}
