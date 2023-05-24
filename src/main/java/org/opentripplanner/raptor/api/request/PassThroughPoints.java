package org.opentripplanner.raptor.api.request;

/**
 * Provides information for pass-through points used in the search.
 */
public interface PassThroughPoints {
  /** Implementation that answers negative for all stops. */
  PassThroughPoints NO_PASS_THROUGH_POINTS = new PassThroughPoints() {
    @Override
    public boolean isPassThroughPoint(final int passThroughIndex, final int stop) {
      return false;
    }

    @Override
    public int size() {
      return 0;
    }
  };

  /**
   * If a certain stop is a pass-through point of a certain position in the trip.
   *
   * @param passThroughIndex the index position of the stop in the list of pass-through points, zero-based
   * @param stop the stop index to check
   * @return boolean true if the stop is a pass-through point on the specific position
   */
  boolean isPassThroughPoint(int passThroughIndex, int stop);

  /**
   * Get the number of pass-through points in the collection.
   */
  int size();
}
