package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * Represents a driver's carpool journey with planned route, timing, and passenger capacity.
 * <p>
 * A carpool trip models a driver offering their vehicle journey for passengers to join. It includes
 * the driver's planned route as a sequence of stops, available seating capacity, and timing
 * constraints including a deviation budget that allows the driver to slightly adjust their route
 * to accommodate passengers.
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><strong>Origin/Destination Areas:</strong> Start and end zones for the driver's journey</li>
 *   <li><strong>Stops:</strong> Ordered sequence of waypoints along the route where passengers
 *       can be picked up or dropped off. Stops are dynamically updated as bookings occur.</li>
 *   <li><strong>Deviation Budget:</strong> Maximum additional time the driver is willing to spend
 *       to pick up/drop off passengers (e.g., 5 minutes). This represents the driver's flexibility.</li>
 *   <li><strong>Available Seats:</strong> Current passenger capacity remaining in the vehicle</li>
 * </ul>
 *
 * <h2>Data Source</h2>
 * <p>
 * Trips are typically created from SIRI-ET messages provided by external carpooling platforms.
 * The platform manages driver registrations, trip creation, and real-time updates as passengers
 * book or cancel rides.
 *
 * <h2>Immutability</h2>
 * <p>
 * CarpoolTrip instances are immutable. Updates to trip state (e.g., adding a booked passenger)
 * require creating a new trip instance via {@link CarpoolTripBuilder} and upserting it to the
 * {@link org.opentripplanner.ext.carpooling.CarpoolingRepository}.
 *
 * <h2>Usage in Routing</h2>
 * <p>
 * The routing algorithm uses trips to find compatible matches for passenger requests:
 * <ol>
 *   <li>Filters check basic compatibility (capacity, timing, direction)</li>
 *   <li>Insertion strategy finds optimal pickup/dropoff positions along the route</li>
 *   <li>Validators ensure constraints (capacity timeline, deviation budget) are satisfied</li>
 * </ol>
 *
 * @see CarpoolStop for individual stop details
 * @see CarpoolTripBuilder for constructing trip instances
 * @see org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater for trip updates
 */
public class CarpoolTrip
  extends AbstractTransitEntity<CarpoolTrip, CarpoolTripBuilder>
  implements LogInfo {

  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final String provider;

  // The amount of time the trip can deviate from the scheduled time in order to pick up or drop off
  // a passenger.
  private final Duration deviationBudget;
  private final int availableSeats;

  // Ordered list of stops along the carpool route where passengers can be picked up or dropped off
  private final List<CarpoolStop> stops;

  public CarpoolTrip(CarpoolTripBuilder builder) {
    super(builder.getId());
    this.startTime = builder.startTime();
    this.endTime = builder.endTime();
    this.provider = builder.provider();
    this.availableSeats = builder.availableSeats();
    this.deviationBudget = builder.deviationBudget();
    this.stops = Collections.unmodifiableList(builder.stops());
  }

  /**
   * Returns the origin stop (first stop in the trip).
   *
   * @return the origin stop
   * @throws IllegalStateException if the trip has no stops
   */
  public CarpoolStop getOrigin() {
    if (stops.isEmpty()) {
      throw new IllegalStateException("Trip has no stops");
    }
    return stops.get(0);
  }

  /**
   * Returns the destination stop (last stop in the trip).
   *
   * @return the destination stop
   * @throws IllegalStateException if the trip has no stops
   */
  public CarpoolStop getDestination() {
    if (stops.isEmpty()) {
      throw new IllegalStateException("Trip has no stops");
    }
    return stops.get(stops.size() - 1);
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public String provider() {
    return provider;
  }

  public Duration deviationBudget() {
    return deviationBudget;
  }

  public int availableSeats() {
    return availableSeats;
  }

  /**
   * Returns the ordered sequence of stops along the carpool route.
   * <p>
   * Stops include both the driver's originally planned stops and any dynamically added stops
   * for passenger pickups and dropoffs. The list is ordered by sequence number, representing
   * the order in which stops are visited along the route.
   *
   * @return an immutable list of stops along the carpool route, ordered by sequence number,
   *         never null but may be empty for trips with no intermediate stops
   */
  public List<CarpoolStop> stops() {
    return stops;
  }

  /**
   * Builds the full list of route points including origin area, all stops, and destination area.
   * <p>
   * This list represents the complete path of the carpool trip, useful for distance and
   * direction calculations during filtering and matching.
   *
   * @return a list of coordinates representing the full route of the trip
   */
  public List<WgsCoordinate> routePoints() {
    return stops.stream().map(CarpoolStop::getCoordinate).toList();
  }

  /**
   * Calculates the number of passengers in the vehicle after visiting the specified position.
   * <p>
   * Position semantics:
   * - Position 0: Before any stops → 0 passengers
   * - Position N: After Nth stop → cumulative passenger delta up to stop N
   *
   * @param position The position index (0 = before any stops, 1 = after first stop, etc.)
   * @return Number of passengers after this position
   * @throws IllegalArgumentException if position is negative or greater than stops.size()
   */
  public int getPassengerCountAtPosition(int position) {
    if (position < 0) {
      throw new IllegalArgumentException("Position must be non-negative, got: " + position);
    }

    if (position > stops.size()) {
      throw new IllegalArgumentException(
        "Position " + position + " exceeds valid range (0 to " + stops.size() + ")"
      );
    }

    // Position 0 is before any stops
    if (position == 0) {
      return 0;
    }

    // Accumulate passenger deltas up to this position
    int count = 0;
    for (int i = 0; i < position; i++) {
      count += stops.get(i).getPassengerDelta();
    }

    return count;
  }

  /**
   * Checks if there's capacity to add passengers throughout a range of positions.
   * <p>
   * This validates that adding passengers won't exceed vehicle capacity at any point
   * between pickup and dropoff positions.
   *
   * @param pickupPosition The pickup position (1-indexed, inclusive)
   * @param dropoffPosition The dropoff position (1-indexed, exclusive)
   * @param additionalPassengers Number of passengers to add (typically 1)
   * @return true if capacity is available throughout the entire range, false otherwise
   */
  public boolean hasCapacityForInsertion(
    int pickupPosition,
    int dropoffPosition,
    int additionalPassengers
  ) {
    int pickupPassengers = getPassengerCountAtPosition(pickupPosition - 1);
    if (pickupPassengers + additionalPassengers > availableSeats) {
      return false;
    }

    for (int pos = pickupPosition; pos < dropoffPosition; pos++) {
      int currentPassengers = getPassengerCountAtPosition(pos);
      if (currentPassengers + additionalPassengers > availableSeats) {
        return false;
      }
    }

    return true;
  }

  @Nullable
  @Override
  public String logName() {
    return getId().toString();
  }

  @Override
  public boolean sameAs(CarpoolTrip other) {
    return (
      getId().equals(other.getId()) &&
      startTime.equals(other.startTime) &&
      endTime.equals(other.endTime) &&
      stops.equals(other.stops)
    );
  }

  @Override
  public TransitBuilder<CarpoolTrip, CarpoolTripBuilder> copy() {
    return new CarpoolTripBuilder(this);
  }
}
