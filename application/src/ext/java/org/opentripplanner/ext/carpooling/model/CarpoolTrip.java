package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.organization.ContactInfo;

/**
 * Represents a driver's carpool journey with planned route, timing, and passenger capacity.
 * <p>
 * A carpool trip models a driver offering their vehicle journey for passengers to join. It includes
 * the driver's planned route as a sequence of stops, total vehicle capacity, and timing
 * constraints including a deviation budget that allows the driver to slightly adjust their route
 * to accommodate passengers.
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><strong>Origin/Destination Areas:</strong> Start and end zones for the driver's journey</li>
 *   <li><strong>Stops:</strong> Ordered sequence of waypoints along the route where passengers
 *       can be picked up or dropped off. Stops are dynamically updated as bookings occur.</li>
 *   <li><strong>Total Capacity:</strong> Number of seats in the car, including the driver seat</li>
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

  /** Default total capacity (including driver) when no capacity information is provided. */
  public static final int DEFAULT_TOTAL_CAPACITY = 5;

  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final String provider;
  private final int totalCapacity;

  // Ordered list of stops along the carpool route where passengers can be picked up or dropped off
  private final List<CarpoolStop> stops;

  @Nullable
  private final ContactInfo publicContactInformation;

  public CarpoolTrip(CarpoolTripBuilder builder) {
    super(builder.getId());
    this.startTime = builder.startTime();
    this.endTime = builder.endTime();
    this.provider = builder.provider();
    this.totalCapacity = builder.totalCapacity();
    this.stops = Collections.unmodifiableList(builder.stops());
    this.publicContactInformation = builder.publicContactInformation();
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

  /**
   * Returns the latest expected arrival time of the destination stop if available, otherwise
   * falls back to {@link #endTime()}.
   *
   * @return the latest expected end time of the trip
   */
  public ZonedDateTime latestEndTime() {
    var latest = getDestination().getLatestExpectedArrivalTime();
    return latest != null ? latest : endTime;
  }

  public String provider() {
    return provider;
  }

  /**
   * @return Total number of seats in the vehicle, including the driver seat
   */
  public int totalCapacity() {
    return totalCapacity;
  }

  /**
   * Returns the ordered sequence of stops along the carpool route.
   * <p>
   * Stops include both the driver's originally planned stops and any dynamically added stops
   * for passenger pickups and dropoffs. The list is ordered by visit order along the route:
   * the first element is the origin and the last is the destination.
   *
   * @return an immutable list of stops along the carpool route, in visit order; never null,
   *         and always contains at least the origin and destination
   */
  public List<CarpoolStop> stops() {
    return stops;
  }

  @Nullable
  public ContactInfo publicContactInformation() {
    return publicContactInformation;
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
   * Returns the number of passengers onboard the vehicle when departing the given stop.
   *
   * @param stopIndex The 0-based index of the stop in the stop list
   * @return Number of passengers onboard when departing from this stop
   * @throws IllegalArgumentException if stopIndex is out of bounds
   */
  public int getPassengerCountAtDepartureOfStop(int stopIndex) {
    if (stopIndex < 0 || stopIndex >= stops.size()) {
      throw new IllegalArgumentException(
        "Stop index " + stopIndex + " is out of bounds (0 to " + (stops.size() - 1) + ")"
      );
    }

    return stops.get(stopIndex).getOnboardCount();
  }

  /**
   * Checks if there's capacity to insert a passenger at the given pickup and dropoff positions
   * in the modified route.
   * <p>
   * The positions are 0-based indices of the passenger's pickup and dropoff stops in the
   * modified route (the route after the passenger's stops have been inserted). For example,
   * with original stops [Origin, A, B, Destination] and pickupPosition=1, dropoffPosition=3:
   * the modified route is [Origin, Pickup, A, Dropoff, B, Destination].
   * All stops between (inclusive) pickupPosition - 1 and dropoffPosition - 2 are checked for capacity.
   * In the example this is between stops 0 and 1, meaning that stops Origin and A need to have sufficient
   * capacity for {@code additionalPassengers} extra passengers.
   * <p>
   *
   * @param pickupPosition 0-based index of the passenger's pickup in the modified route.
   *        Must be >= 1 (position 0 is the driver's origin).
   * @param dropoffPosition 0-based index of the passenger's dropoff in the modified route.
   *        Must be > pickupPosition.
   * @param additionalPassengers Number of passengers to add (typically 1)
   * @return true if capacity is available throughout the entire range, false otherwise
   * @throws IllegalArgumentException if pickupPosition < 1 or dropoffPosition <= pickupPosition
   */
  public boolean hasCapacityForInsertion(
    int pickupPosition,
    int dropoffPosition,
    int additionalPassengers
  ) {
    if (pickupPosition < 1) {
      throw new IllegalArgumentException(
        "pickupPosition must be >= 1 (position 0 is the driver's origin), got: " + pickupPosition
      );
    }
    if (dropoffPosition <= pickupPosition) {
      throw new IllegalArgumentException(
        "dropoffPosition must be > pickupPosition, got: pickupPosition=" +
          pickupPosition +
          ", dropoffPosition=" +
          dropoffPosition
      );
    }

    int firstOriginalStop = pickupPosition - 1;
    int lastOriginalStop = dropoffPosition - 2;

    for (int i = firstOriginalStop; i <= lastOriginalStop; i++) {
      if (getPassengerCountAtDepartureOfStop(i) + additionalPassengers > totalCapacity) {
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
      stops.equals(other.stops) &&
      Objects.equals(publicContactInformation, other.publicContactInformation)
    );
  }

  @Override
  public TransitBuilder<CarpoolTrip, CarpoolTripBuilder> copy() {
    return new CarpoolTripBuilder(this);
  }
}
