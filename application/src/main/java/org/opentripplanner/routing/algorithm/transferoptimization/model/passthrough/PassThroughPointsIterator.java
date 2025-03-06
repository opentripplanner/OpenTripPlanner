package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.BitSet;
import java.util.List;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;

/**
 * Iterate over the pass-through points. Note! This implementation iterates backwards starting at the last
 * pass-through point.
 */
class PassThroughPointsIterator {

  private final List<RaptorViaLocation> viaLocations;
  private int index;
  private BitSet current;

  private PassThroughPointsIterator(List<RaptorViaLocation> viaLocations, int c2) {
    this.viaLocations = viaLocations;
    this.index = c2;
    next();
  }

  /**
   * Iterate from the given {@code c2} value (pass-through-point number minus one) towards the
   * beginning.
   */
  static PassThroughPointsIterator tailIterator(List<RaptorViaLocation> viaLocations, int c2) {
    return new PassThroughPointsIterator(viaLocations, c2);
  }

  /**
   * Iterate over the pass-though-points starting at the end/destination and towards the beginning
   * of the points, until the origin is reached.
   */
  static PassThroughPointsIterator tailIterator(List<RaptorViaLocation> viaLocations) {
    return new PassThroughPointsIterator(viaLocations, viaLocations.size());
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
    this.current = index < 0 ? null : viaLocations.get(index).asBitSet();
  }

  boolean isPassThroughPoint(int stopIndex) {
    return current != null && current.get(stopIndex);
  }
}
