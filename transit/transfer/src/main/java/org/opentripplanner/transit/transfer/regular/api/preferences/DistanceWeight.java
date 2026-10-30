package org.opentripplanner.transit.transfer.regular.api.preferences;

import org.opentripplanner.core.model.basic.NormalizedDistance;

/**
 * The weight of a field in the distance between two user preferences.
 * <p>
 * The distance of each field is normalized to its own range: the speed and the reluctance are on a
 * logarithmic scale, and the ratios are linear. The same change of the value gives very different
 * distances. A weight scales the distance of a field, so a reference change of the field is
 * {@link #REFERENCE_DISTANCE}. The fields of a preferences class then count the same for the
 * reference changes, for example 1.3 to 1.4 m/s for the walking speed, 2.0 to 2.5 for the
 * reluctance and 0.5 to 0.6 for the safety factor.
 */
final class DistanceWeight {

  /** The distance a reference change of a field gets. */
  static final double REFERENCE_DISTANCE = 0.1;

  private DistanceWeight() {}

  /** The weight that makes the change from {@code from} to {@code to} the reference distance. */
  static <T extends NormalizedDistance<T>> double of(T from, T to) {
    double distance = from.normalizedDistance(to);
    if (distance <= 0.0) {
      throw new IllegalArgumentException("The reference values must be different: " + from);
    }
    return REFERENCE_DISTANCE / distance;
  }
}
