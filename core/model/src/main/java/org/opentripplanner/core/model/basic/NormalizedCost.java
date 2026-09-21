package org.opentripplanner.core.model.basic;

/// A type safe normalized representation of a cost, like generalized-cost. A cost unit is
/// equivalent of riding transit for 1 seconds. Note! The resolution of the normalized cost is
/// seconds. A cost can not be negative.
/// <p>
/// Perform all calculation using the {@link Cost}, and only convert to this class as the last step.
/// <p>
/// This is an immutable, thread-safe value-object.
public final class NormalizedCost extends Cost {

  /// package private - used by Cost (the only way to create this)
  NormalizedCost(int centiSeconds) {
    super(toCentiSeconds(roundToSeconds(centiSeconds)));
  }

  @Override
  public NormalizedCost normalize() {
    return this;
  }
}
