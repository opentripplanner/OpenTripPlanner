package org.opentripplanner.ext.carpooling.routing;

public class InsertionPosition {

  private final int pickupPos;
  private final int dropOffPos;

  /**
   * Represents a pickup and dropoff position pair that passed heuristic validation.
   * <p>
   * This is an intermediate value used between finding viable positions (via heuristics)
   * and evaluating them (via A* routing). Positions are 0-based indices of the passenger's
   * pickup and dropoff stops in the modified route (the route after the passenger's stops
   * have been inserted into the carpool trip).
   *
   * @param pickupPos  0-based index of the passenger's pickup in the modified route
   * @param dropoffPos 0-based index of the passenger's dropoff in the modified route (always > pickupPos)
   */
  public InsertionPosition(int pickupPos, int dropoffPos) {
    if (dropoffPos <= pickupPos) {
      throw new IllegalArgumentException(
        "dropoffPos (%d) must be greater than pickupPos (%d)".formatted(dropoffPos, pickupPos)
      );
    }
    this.pickupPos = pickupPos;
    this.dropOffPos = dropoffPos;
  }

  public int pickupPos() {
    return pickupPos;
  }

  public int dropoffPos() {
    return dropOffPos;
  }

  /**
   * Maps an index in the original route to the corresponding index in the
   * modified route after passenger stops have been inserted.
   * <p>
   * When a passenger pickup and dropoff are inserted into a route, all subsequent
   * indices shift. This method calculates the new index for an original route point.
   *
   * @param originalIndex Index in original route (before passenger insertion)
   * @param pickupPos 0-based index of the passenger's pickup in the modified route
   * @param dropoffPos 0-based index of the passenger's dropoff in the modified route
   * @return Corresponding index in modified route (after passenger insertion)
   */
  public static int mapOriginalIndex(int originalIndex, int pickupPos, int dropoffPos) {
    int modifiedIndex = originalIndex;

    // Account for pickup insertion
    // If the original point was at or after pickupPos, it shifts by 1
    if (originalIndex >= pickupPos) {
      modifiedIndex++;
    }

    // Account for dropoff insertion
    // After pickup insertion, check if the shifted index is at or after dropoffPos
    if (modifiedIndex >= dropoffPos) {
      modifiedIndex++;
    }

    return modifiedIndex;
  }
}
