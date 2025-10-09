package org.opentripplanner.ext.carpooling.util;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Tracks passenger counts at each position in a carpool route.
 * <p>
 * Index i represents the number of passengers AFTER position i (before the next segment).
 * <p>
 * Example:
 * <pre>
 * Position:     0(Boarding)  1(Stop1)  2(Stop2)  3(Alighting)
 * Delta:              -         +2        -1           -
 * Passengers:    0        →  2      →  1       →  0
 * Timeline:     [0,           2,        1,           0]
 * </pre>
 */
public class PassengerCountTimeline {

  private final List<Integer> counts;
  private final int capacity;

  private PassengerCountTimeline(List<Integer> counts, int capacity) {
    this.counts = counts;
    this.capacity = capacity;
  }

  /**
   * Builds a passenger count timeline from a carpool trip.
   *
   * @param trip The carpool trip
   * @return Timeline tracking passenger counts at each position
   */
  public static PassengerCountTimeline build(CarpoolTrip trip) {
    List<Integer> timeline = new ArrayList<>();
    int currentPassengers = 0;

    // Position 0: Boarding (no passengers yet)
    timeline.add(currentPassengers);

    // Add passenger delta for each stop
    for (CarpoolStop stop : trip.stops()) {
      currentPassengers += stop.getPassengerDelta();
      timeline.add(currentPassengers);
    }

    // Position N+1: Alighting (all passengers leave)
    currentPassengers = 0;
    timeline.add(currentPassengers);

    return new PassengerCountTimeline(timeline, trip.availableSeats());
  }

  /**
   * Gets the passenger count at a specific position.
   *
   * @param position Position index
   * @return Number of passengers after this position
   */
  public int getPassengerCount(int position) {
    if (position < 0 || position >= counts.size()) {
      throw new IndexOutOfBoundsException(
        "Position " + position + " out of bounds (size: " + counts.size() + ")"
      );
    }
    return counts.get(position);
  }

  /**
   * Gets the vehicle capacity.
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Gets the number of positions tracked.
   */
  public int size() {
    return counts.size();
  }

  /**
   * Checks if there's available capacity at a specific position.
   *
   * @param position Position to check
   * @return true if there's at least one available seat
   */
  public boolean hasCapacity(int position) {
    return getPassengerCount(position) < capacity;
  }

  /**
   * Checks if there's capacity for a specific number of additional passengers.
   *
   * @param position Position to check
   * @param additionalPassengers Number of passengers to add
   * @return true if there's capacity for the additional passengers
   */
  public boolean hasCapacityFor(int position, int additionalPassengers) {
    return getPassengerCount(position) + additionalPassengers <= capacity;
  }

  /**
   * Checks if there's capacity throughout a range of positions.
   * <p>
   * This is useful for validating that adding a passenger between pickup and dropoff
   * won't exceed capacity at any point along the route.
   *
   * @param startPosition First position (inclusive)
   * @param endPosition Last position (exclusive)
   * @param additionalPassengers Number of passengers to add
   * @return true if capacity is available throughout the range
   */
  public boolean hasCapacityInRange(int startPosition, int endPosition, int additionalPassengers) {
    for (int pos = startPosition; pos < endPosition && pos < counts.size(); pos++) {
      if (!hasCapacityFor(pos, additionalPassengers)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the available seat count at a position.
   *
   * @param position Position to check
   * @return Number of available seats (capacity - current passengers)
   */
  public int getAvailableSeats(int position) {
    return capacity - getPassengerCount(position);
  }

  @Override
  public String toString() {
    return "PassengerCountTimeline{counts=" + counts + ", capacity=" + capacity + "}";
  }
}
