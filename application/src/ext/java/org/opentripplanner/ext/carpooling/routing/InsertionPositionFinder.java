package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screens pickup/dropoff position pairs on capacity and a beeline delay heuristic, so only positions
 * worth routing reach {@link InsertionEvaluator}.
 */
public class InsertionPositionFinder {

  private static final Logger LOG = LoggerFactory.getLogger(InsertionPositionFinder.class);

  private final BeelineEstimator beelineEstimator;

  public InsertionPositionFinder() {
    this(new BeelineEstimator());
  }

  public InsertionPositionFinder(BeelineEstimator beelineEstimator) {
    this.beelineEstimator = beelineEstimator;
  }

  /**
   * The positions that pass capacity and the beeline delay check, which is a lower bound on the
   * routed delay — so no feasible insertion is lost. The bound holds because the detour is beelined
   * between the resolved vertices the driver is actually routed between, not the trip's declared
   * route points.
   *
   * @param passengerPickup already snapped to a car-accessible vertex.
   * @param passengerDropoff already snapped to a car-accessible vertex.
   * @param stopDuration dwell time at each intermediate stop.
   */
  public List<InsertionPosition> findViablePositions(
    RoutedCarpoolTrip routedTrip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Duration stopDuration
  ) {
    CarpoolTrip trip = routedTrip.trip();
    List<WgsCoordinate> routePoints = routedTrip.vertexCoordinates();
    Duration[] baselineCumulative = routedTrip.cumulativeArrivals(stopDuration);

    List<InsertionPosition> viable = new ArrayList<>();

    // 0-based indices in the modified route: pickup cannot be the driver's origin, and dropoff must
    // follow it.
    for (int pickupPos = 1; pickupPos < routePoints.size(); pickupPos++) {
      for (int dropoffPos = pickupPos + 1; dropoffPos <= routePoints.size(); dropoffPos++) {
        if (!trip.hasCapacityForInsertion(pickupPos, dropoffPos, 1)) {
          LOG.trace(
            "Insertion at pickup={}, dropoff={} rejected by capacity check",
            pickupPos,
            dropoffPos
          );
          continue;
        }

        var position = new InsertionPosition(pickupPos, dropoffPos);
        if (
          !passesBeelineDelayCheck(
            routedTrip,
            position,
            baselineCumulative,
            passengerPickup,
            passengerDropoff,
            stopDuration
          )
        ) {
          LOG.trace(
            "Insertion at pickup={}, dropoff={} rejected by beeline delay heuristic",
            pickupPos,
            dropoffPos
          );
          continue;
        }

        viable.add(position);
      }
    }

    return viable;
  }

  /**
   * True if the insertion might satisfy the delay constraints. Untouched legs keep their baseline
   * duration; only the detour segments around the inserted points are beelined.
   */
  private boolean passesBeelineDelayCheck(
    RoutedCarpoolTrip routedTrip,
    InsertionPosition position,
    Duration[] baselineCumulative,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Duration stopDuration
  ) {
    List<WgsCoordinate> modifiedCoords = new ArrayList<>(routedTrip.vertexCoordinates());
    modifiedCoords.add(position.pickupPos(), passengerPickup);
    modifiedCoords.add(position.dropoffPos(), passengerDropoff);

    Duration[] modifiedSegmentDurations = new Duration[modifiedCoords.size() - 1];
    for (int i = 0; i < modifiedSegmentDurations.length; i++) {
      int baselineIndex = position.baselineSegmentIndex(i);
      modifiedSegmentDurations[i] = baselineIndex >= 0
        ? routedTrip.legDurations()[baselineIndex]
        : beelineEstimator.estimateDuration(modifiedCoords.get(i), modifiedCoords.get(i + 1));
    }

    Duration[] modifiedCumulative = GraphPathUtils.calculateCumulativeDurations(
      modifiedSegmentDurations,
      stopDuration
    );

    return PassengerDelayConstraints.satisfiesConstraints(
      baselineCumulative,
      modifiedCumulative,
      position.pickupPos(),
      position.dropoffPos(),
      routedTrip.trip().stops()
    );
  }
}
