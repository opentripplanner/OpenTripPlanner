package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds viable insertion positions for a passenger in a carpool trip using fast heuristics.
 * <p>
 * This class performs early-stage filtering to identify pickup/dropoff position pairs that
 * are worth evaluating with expensive A* routing. It validates positions using:
 * <ul>
 *   <li>Capacity constraints - ensures available seats throughout the journey</li>
 *   <li>Beeline delay heuristic - optimistic straight-line time estimates</li>
 * </ul>
 * <p>
 * This follows the established OTP pattern of separating candidate generation from evaluation,
 * similar to {@code TransferGenerator} and {@code StreetNearbyStopFinder}.
 */
public class InsertionPositionFinder {

  private static final Logger LOG = LoggerFactory.getLogger(InsertionPositionFinder.class);

  private final BeelineEstimator beelineEstimator;

  /**
   * Creates a finder with default estimator.
   */
  public InsertionPositionFinder() {
    this(new BeelineEstimator());
  }

  /**
   * Creates a finder with specified estimator.
   *
   * @param beelineEstimator Estimator for beeline travel times
   */
  public InsertionPositionFinder(BeelineEstimator beelineEstimator) {
    this.beelineEstimator = beelineEstimator;
  }

  /**
   * Finds insertion positions that pass validation and beeline checks.
   * This is done BEFORE any expensive routing to eliminate positions early.
   *
   * @param trip The carpool trip being evaluated
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @param stopDuration Dwell time added at each intermediate stop; used by the beeline delay
   *                     heuristic so its cumulative-time estimates match the per-stop budget
   *                     check used downstream
   * @return List of viable insertion positions (may be empty)
   */
  public List<InsertionPosition> findViablePositions(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Duration stopDuration
  ) {
    List<WgsCoordinate> routePoints = trip.routePoints();

    Duration[] beelineTimes = beelineEstimator.calculateCumulativeTimes(routePoints, stopDuration);

    List<InsertionPosition> viable = new ArrayList<>();

    // pickupPos/dropoffPos are 0-based indices of the passenger's stops in the modified route.
    // Pickup cannot be at index 0 (that's the driver's origin).
    for (int pickupPos = 1; pickupPos < routePoints.size(); pickupPos++) {
      // Dropoff must be after pickup. Max is routePoints.size() (appended after all original stops except the last).
      for (int dropoffPos = pickupPos + 1; dropoffPos <= routePoints.size(); dropoffPos++) {
        if (!trip.hasCapacityForInsertion(pickupPos, dropoffPos, 1)) {
          LOG.trace(
            "Insertion at pickup={}, dropoff={} rejected by capacity check",
            pickupPos,
            dropoffPos
          );
          continue;
        }

        if (
          !passesBeelineDelayCheck(
            routePoints,
            beelineTimes,
            passengerPickup,
            passengerDropoff,
            pickupPos,
            dropoffPos,
            trip,
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

        viable.add(new InsertionPosition(pickupPos, dropoffPos));
      }
    }

    return viable;
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
   * @param pickupPos 0-based index of the passenger's pickup in the modified route
   * @param dropoffPos 0-based index of the passenger's dropoff in the modified route
   * @param trip The carpool trip being evaluated
   * @return true if insertion might satisfy delay constraints (proceed with A* routing)
   */
  private boolean passesBeelineDelayCheck(
    List<WgsCoordinate> originalCoords,
    Duration[] originalBeelineTimes,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    int pickupPos,
    int dropoffPos,
    CarpoolTrip trip,
    Duration stopDuration
  ) {
    // Build modified coordinate list with passenger stops inserted
    List<WgsCoordinate> modifiedCoords = new ArrayList<>(originalCoords);
    modifiedCoords.add(pickupPos, passengerPickup);
    modifiedCoords.add(dropoffPos, passengerDropoff);

    // Calculate beeline times for modified route
    Duration[] modifiedBeelineTimes = beelineEstimator.calculateCumulativeTimes(
      modifiedCoords,
      stopDuration
    );

    // If even the optimistic beeline estimate exceeds a stop's budget, actual routing will too
    return PassengerDelayConstraints.satisfiesConstraints(
      originalBeelineTimes,
      modifiedBeelineTimes,
      pickupPos,
      dropoffPos,
      trip.stops()
    );
  }
}
