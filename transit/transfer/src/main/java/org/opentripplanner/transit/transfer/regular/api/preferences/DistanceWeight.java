package org.opentripplanner.transit.transfer.regular.api.preferences;

import org.opentripplanner.core.model.basic.NormalizedDistance;

/**
 * The weight of a field in the distance between two user preferences.
 * <p>
 * The distance of each field is normalized to its own range: the speed and the reluctance are on a
 * logarithmic scale, and the ratios are linear. The same change of the value gives very different
 * distances. A weight scales the distance of a field, so a reference change of the field is
 * {@link #REFERENCE_DISTANCE}, for example 1.3 to 1.4 m/s for the walking speed, 2.0 to 2.5 for the
 * reluctance and 0.5 to 0.6 for the safety factor.
 * <p>
 * The reference distance is only a way to make the fields comparable. How much a field matters in
 * practice, and so how it is scaled further, and the asymmetric weights of the safety, are
 * documented in the {@code normalizedDistance} method of each preferences class.
 */
final class DistanceWeight {

  /** The distance a reference change of a field gets. */
  static final double REFERENCE_DISTANCE = 0.1;

  private DistanceWeight() {}

  /**
   * The weight that makes the change from {@code from} to {@code to} the reference distance. The
   * type must have a symmetric distance, an asymmetric one would give a weight that depends on the
   * order of the arguments.
   */
  static <T extends NormalizedDistance<T>> double of(T from, T to) {
    double distance = from.normalizedDistance(to);
    if (distance <= 0.0) {
      throw new IllegalArgumentException("The reference values must be different: " + from);
    }
    return REFERENCE_DISTANCE / distance;
  }
}
