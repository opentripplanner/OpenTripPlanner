package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.BitSet;
import java.util.List;
import org.opentripplanner.raptor.api.request.PassThroughPoint;

/**
 * Iterate over the pass-through points. Note! This implementation iterates backwards starting at the last
 * pass-through point.
 */
class PassThroughPointsIterator {

  private final List<PassThroughPoint> passThroughPoints;
  private int index;
  private BitSet current;

  private PassThroughPointsIterator(List<PassThroughPoint> passThroughPoints, int c2) {
    this.passThroughPoints = passThroughPoints;
    this.index = c2;
    next();
  }

  /**
   * Iterate from the given {@code c2} value (pass-through-point number minus one) towards the
   * beginning.
   */
  static PassThroughPointsIterator tailIterator(List<PassThroughPoint> passThroughPoints, int c2) {
    return new PassThroughPointsIterator(passThroughPoints, c2);
  }

  /**
   * Iterate over the pass-though-points starting at the end/destination and towards the beginning
   * of the points, until the origin is reached.
   */
  static PassThroughPointsIterator tailIterator(List<PassThroughPoint> passThroughPoints) {
    return new PassThroughPointsIterator(passThroughPoints, passThroughPoints.size());
  }

  /**
   * The current c2 value reached by the iterator.
   */
  int currC2() {
    return index + 1;
  }

  /**
   * Go to the next element (move to the previous pass-though-point)
   */
  void next() {
    --index;
    this.current = index < 0 ? null : passThroughPoints.get(index).asBitSet();
  }

  boolean isPassThroughPoint(int stopIndex) {
    return current != null && current.get(stopIndex);
  }
}
