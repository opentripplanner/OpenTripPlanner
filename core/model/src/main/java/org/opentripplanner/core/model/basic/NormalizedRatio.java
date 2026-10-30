package org.opentripplanner.core.model.basic;

import org.opentripplanner.utils.lang.DoubleUtils;

/// A [Ratio] where the value is rounded to a step of 5/100, so it can be used as a part of a cache
/// key. The normalized ratios have a [#normalizedDistance(NormalizedRatio)], which is used to find
/// how much two cache keys differ.
///
/// THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
public final class NormalizedRatio extends Ratio implements NormalizedDistance<NormalizedRatio> {

  /// package private - use {@link Ratio#normalize()} to create a normalized ratio
  NormalizedRatio(double value) {
    super(DoubleUtils.roundToStep(value, 0.05));
  }

  @Override
  public NormalizedRatio normalize() {
    return this;
  }

  /// The distance is the difference between the two ratios, a value in the range [0.0, 1.0].
  @Override
  public double normalizedDistance(NormalizedRatio other) {
    return Math.abs(asDouble() - other.asDouble());
  }
}
