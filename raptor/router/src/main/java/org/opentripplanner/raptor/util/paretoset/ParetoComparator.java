package org.opentripplanner.raptor.util.paretoset;

/// Compares two elements in a {@link ParetoSet} for Pareto dominance.
///
/// A comparison between a `left` and `right` element can produce four mutually exclusive outcomes:
///
/// - {@link ParetoDominance#LEFT}
/// - {@link ParetoDominance#RIGHT}
/// - {@link ParetoDominance#MUTUAL}
/// - {@link ParetoDominance#NONE}
///
/// Implementations only need to provide one directional check by implementing
/// {@link #leftDominanceExist(Object, Object)}.
///
/// @param <T> the Pareto set element type.
///
@FunctionalInterface
public interface ParetoComparator<T> {
  /**
   * Returns {@code true} if at least one criterion in {@code left} is better then the
   * corresponding criterion in {@code right}.
   */
  boolean leftDominanceExist(T left, T right);

  /**
   * Returns {@code true} if either element dominates the other.
   */
  default boolean dominanceExist(T left, T right) {
    return leftDominanceExist(left, right) || leftDominanceExist(right, left);
  }

  /**
   * Compares {@code left} and {@code right} and returns the corresponding {@link ParetoDominance}
   * outcome.
   */
  default ParetoDominance compare(T left, T right) {
    final boolean l = leftDominanceExist(left, right);
    final boolean r = leftDominanceExist(right, left);
    return ParetoDominance.of(l, r);
  }
}
