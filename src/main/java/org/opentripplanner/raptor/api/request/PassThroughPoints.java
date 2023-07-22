package org.opentripplanner.raptor.api.request;

import java.util.function.IntConsumer;

/**
 * Provides information for pass-through points used in the search.
 */
public interface PassThroughPoints {
  /** Implementation that answers negative for all stops. */
  PassThroughPoints NOOP = new PassThroughPoints() {
    @Override
    public boolean isPassThroughPoint(final int stop) {
      return false;
    }

    @Override
    public void updateC2Value(int c2, IntConsumer update) {
      throw new UnsupportedOperationException("This should never be called");
    }

    @Override
    public int size() {
      return 0;
    }
  };

  /**
   * If a certain stop is a pass-through point of a certain position in the trip.
   *
   * @param stopIndex the stop index to check
   * @return boolean true if the stop is a pass-through point on the specific stop
   */
  boolean isPassThroughPoint(int stopIndex);

  void updateC2Value(int c2, IntConsumer update);

  /**
   * Get the number of pass-through points in the collection.
   */
  int size();
}
